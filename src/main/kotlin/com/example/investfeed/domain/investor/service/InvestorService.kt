package com.example.investfeed.domain.investor.service

import com.example.investfeed.common.util.MarketTimeUtil
import com.example.investfeed.domain.investor.dto.req.InvestorListReq
import com.example.investfeed.domain.investor.dto.req.InvestorStreamReq
import com.example.investfeed.domain.investor.dto.res.InvestorListItem
import com.example.investfeed.domain.investor.dto.res.InvestorListRes
import com.example.investfeed.global.constant.RedisKeyPrefix
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.price.client.PriceClient
import com.example.investfeed.kiwoom.price.dto.req.KiwoomInvestorTradeCloseMarketReq
import com.example.investfeed.kiwoom.price.dto.req.KiwoomInvestorTradeOpenMarketReq
import com.example.investfeed.kiwoom.price.dto.res.KiwoomInvestorTradeCloseMarketItemList
import com.example.investfeed.kiwoom.price.dto.res.KiwoomInvestorTradeCloseMarketRes
import com.example.investfeed.kiwoom.price.dto.res.KiwoomInvestorTradeOpenMarketItemList
import com.example.investfeed.kiwoom.stock.client.StockSocketClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockStream
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockStreamReq
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Collections.emptyList

@Service
class InvestorService(
    private val priceClient: PriceClient,
    private val stockSocketClient: StockSocketClient,
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val holidayService: HolidayService,
) {
    private val CACHE_PREFIX = RedisKeyPrefix.INVESTOR_CLOSE_MARKET.prefix

    companion object {
        private val ALL_COMBINATIONS = listOf("6" to "1", "6" to "2", "7" to "1", "7" to "2")

        private val SCHEDULE_RUN_START: LocalTime = MarketTimeUtil.KRX_TRADE_CLOSE  // 15:36
        private val SCHEDULE_RUN_END: LocalTime = LocalTime.of(21, 0)
        /** 다음 거래일 갱신 재개 시각. 스케줄러 재시작(15:36) + 여유 5분 */
        private val NEXT_DAY_REFRESH_TIME: LocalTime = LocalTime.of(15, 41)
    }

    /**
     * 투자자별 장마감 순위 캐시의 동적 TTL 계산.
     *
     * - 스케줄러 실행 구간(15:36 ~ 21:00) 저장: 다음 분 + 1분 여유 (매 분 덮어쓰기 사이클)
     * - 그 외 시각(장 외 시간대/주말/휴일) 저장: 다음 거래일 15:41 까지
     *   → 15:41 = 스케줄러 재시작(15:36) + 5분 여유. 첫 덮어쓰기 타이밍 공백 방지.
     */
    internal fun ttlUntilNextInvestorUpdate(now: LocalDateTime): Duration {
        val nowTime = now.toLocalTime()
        val target: LocalDateTime =
            if (!nowTime.isBefore(SCHEDULE_RUN_START) && nowTime.isBefore(SCHEDULE_RUN_END)) {
                // 스케줄러 실행 중: 다음 분 + 1분 여유
                now.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
            } else {
                // 장 외 시간/주말/휴일: 다음 거래일 15:41
                val baseDate: LocalDate =
                    if (!nowTime.isBefore(SCHEDULE_RUN_END)) now.toLocalDate()  // 21:00 이후 → 오늘 이후 거래일
                    else now.toLocalDate().minusDays(1)  // 15:36 이전 → 어제 이후 거래일(오늘 포함)
                holidayService.nextTradingDay(baseDate).atTime(NEXT_DAY_REFRESH_TIME)
            }
        val duration = Duration.between(now, target)
        // 안전 하한: 음수/0 방지 (극히 짧은 시간이라도 캐시되도록)
        return if (duration.isNegative || duration.isZero) Duration.ofMinutes(1) else duration
    }

    fun investorList(
        req: InvestorListReq
    ): InvestorListRes? {
        val now = LocalTime.now()

        if (MarketTimeUtil.isKrxTradeClose(now)) {
            return getCloseMarketWithCache(req, now)
        }

        return buildOpenMarketResult(req)
    }

    private fun getCloseMarketWithCache(req: InvestorListReq, now: LocalTime): InvestorListRes? {
        val cacheKey = "$CACHE_PREFIX${req.orgnTp}:${req.trdeTp}"

        redisTemplate.opsForValue().get(cacheKey)?.let { cached ->
            return objectMapper.readValue(cached, InvestorListRes::class.java)
        }

        val rawRes = refreshCloseMarketCache(now)
        return buildFromRaw(rawRes, req.orgnTp, req.trdeTp)
    }

    /**
     * 4조합(외국인/기관 × 매수/매도) 의 Redis 캐시를 현재 시점 기준으로 새로 채운다.
     *
     * - InvestorCloseMarketScheduler 에서 매분 호출 (15:36~21:00 평일)
     * - on-demand fallback 경로에서도 호출 (cache miss 시)
     */
    fun refreshCloseMarketCache(now: LocalTime = LocalTime.now()): KiwoomInvestorTradeCloseMarketRes {
        val rawRes = fetchRawCloseMarket(now)

        val ttl = ttlUntilNextInvestorUpdate(LocalDateTime.now())
        ALL_COMBINATIONS.forEach { (orgnTp, trdeTp) ->
            val result = buildFromRaw(rawRes, orgnTp, trdeTp)
            redisTemplate.opsForValue().set(
                "$CACHE_PREFIX$orgnTp:$trdeTp",
                objectMapper.writeValueAsString(result),
                ttl
            )
        }
        return rawRes
    }

    private fun fetchRawCloseMarket(now: LocalTime): KiwoomInvestorTradeCloseMarketRes {
        return priceClient.investorTradeCloseMarket(
            req = KiwoomInvestorTradeCloseMarketReq(
                mrkt_tp = "000",
                amt_qty_tp = "1",
                trde_tp = "0",
                stex_tp = if (MarketTimeUtil.isNxtTradeClose(now)) "3" else "1",
            )
        )
    }

    private fun buildFromRaw(
        raw: KiwoomInvestorTradeCloseMarketRes,
        orgnTp: String,
        trdeTp: String,
    ): InvestorListRes {
        val sorted: List<KiwoomInvestorTradeCloseMarketItemList> = when (orgnTp) {
            "6" -> when (trdeTp) {
                "1" -> raw.opaf_invsr_trde?.sortedByDescending { it.frgnr_invsr?.toLongOrNull() ?: 0L }?.take(100) ?: emptyList()
                "2" -> raw.opaf_invsr_trde?.sortedBy { it.frgnr_invsr?.toLongOrNull() ?: 0L }?.take(100) ?: emptyList()
                else -> emptyList()
            }
            "7" -> when (trdeTp) {
                "1" -> raw.opaf_invsr_trde?.sortedByDescending { it.orgn?.toLongOrNull() ?: 0L }?.take(100) ?: emptyList()
                "2" -> raw.opaf_invsr_trde?.sortedBy { it.orgn?.toLongOrNull() ?: 0L }?.take(100) ?: emptyList()
                else -> emptyList()
            }
            else -> emptyList()
        }

        val investorList = sorted.map {
            InvestorListItem(
                stkCd = it.stk_cd,
                stkNm = it.stk_nm,
                curPrc = it.cur_prc?.replace(Regex("^[+-]"), ""),
                preSig = it.pre_sig,
                predPre = it.pred_pre,
                fluRt = it.flu_rt,
                accTrdeQty = it.trde_qty,
                netprpsAmt = when (orgnTp) {
                    "6" -> it.frgnr_invsr
                    "7" -> it.orgn
                    else -> null
                },
            )
        }

        return InvestorListRes(investorList = investorList)
    }

    private fun buildOpenMarketResult(req: InvestorListReq): InvestorListRes? {
        val investorList: MutableList<InvestorListItem> = mutableListOf()
        var openResult: MutableList<KiwoomInvestorTradeOpenMarketItemList>

        when (req.orgnTp) {
            "6" -> {
                val kiwoomInvestorTradeDailyRes1 = priceClient.investorTradeOpenMarket(
                    req = KiwoomInvestorTradeOpenMarketReq(
                        mrkt_tp = "000",
                        amt_qty_tp = "1",
                        invsr = req.orgnTp,
                        frgn_all = "0",
                        smtm_netprps_tp = "1",
                        stex_tp = "1",
                    )
                )

                val kiwoomInvestorTradeDailyRes2 = priceClient.investorTradeOpenMarket(
                    req = KiwoomInvestorTradeOpenMarketReq(
                        mrkt_tp = "000",
                        amt_qty_tp = "1",
                        invsr = req.orgnTp,
                        frgn_all = "1",
                        smtm_netprps_tp = "1",
                        stex_tp = "1",
                    )
                )

                val combinedList =
                    (kiwoomInvestorTradeDailyRes1.opmr_invsr_trde ?: emptyList()) +
                    (kiwoomInvestorTradeDailyRes2.opmr_invsr_trde ?: emptyList())

                openResult = combinedList.groupBy { it.stk_cd }.map { (_, items) ->
                    val totalAmt = items.sumOf { item ->
                        val netprps_amt = item.netprps_amt?.trim()?.toLongOrNull() ?: 0L
                        if (netprps_amt == 0L) {
                            val buy = item.buy_amt?.trim()?.toLongOrNull() ?: 0L
                            val sell = item.sell_amt?.trim()?.replace("--", "")?.toLongOrNull() ?: 0L
                            buy - sell
                        } else {
                            netprps_amt
                        }
                    }
                    items.first().apply { this.netprps_amt = totalAmt.toString() }
                }.toMutableList()

                when (req.trdeTp) {
                    "1" -> openResult = openResult.sortedByDescending { it.netprps_amt?.toLongOrNull() ?: 0L }.stream().limit(100).toList()
                    "2" -> openResult = openResult.sortedBy { it.netprps_amt?.toLongOrNull() ?: 0L }.stream().limit(100).toList()
                }

                if (kiwoomInvestorTradeDailyRes1.return_code == 0 && kiwoomInvestorTradeDailyRes2.return_code == 0) {
                    openResult.forEach {
                        investorList.add(
                            InvestorListItem(
                                stkCd = it.stk_cd,
                                stkNm = it.stk_nm,
                                curPrc = it.cur_prc?.replace(Regex("^[+-]"), ""),
                                preSig = it.pre_sig,
                                predPre = it.pred_pre,
                                fluRt = it.flu_rt,
                                accTrdeQty = it.acc_trde_qty,
                                netprpsAmt = it.netprps_amt,
                            )
                        )
                    }
                }
            }
            "7" -> {
                val kiwoomInvestorTradeDailyRes = priceClient.investorTradeOpenMarket(
                    req = KiwoomInvestorTradeOpenMarketReq(
                        mrkt_tp = "000",
                        amt_qty_tp = "1",
                        invsr = req.orgnTp,
                        frgn_all = "0",
                        smtm_netprps_tp = "1",
                        stex_tp = "3",
                    )
                )

                if (kiwoomInvestorTradeDailyRes.return_code == 0) {
                    openResult = kiwoomInvestorTradeDailyRes.opmr_invsr_trde?.map {
                        val netprps_amt = it.netprps_amt?.trim()?.toLongOrNull() ?: 0L
                        if (netprps_amt == 0L) {
                            val buy = it.buy_amt?.trim()?.toLongOrNull() ?: 0L
                            val sell = it.sell_amt?.trim()?.replace("--", "")?.toLongOrNull() ?: 0L

                            it.apply { this.netprps_amt = (buy - sell).toString() }
                        } else {
                            it.apply { this.netprps_amt = netprps_amt.toString() }
                        }
                    }?.toMutableList() ?: emptyList()

                    when (req.trdeTp) {
                        "1" -> openResult = openResult.sortedByDescending { it.netprps_amt?.toLongOrNull() ?: 0L }.stream().limit(100).toList()
                        "2" -> openResult = openResult.sortedBy { it.netprps_amt?.toLongOrNull() ?: 0L }.stream().limit(100).toList()
                    }

                    openResult.forEach {
                        investorList.add(
                            InvestorListItem(
                                stkCd = it.stk_cd,
                                stkNm = it.stk_nm,
                                curPrc = it.cur_prc?.replace(Regex("^[+-]"), ""),
                                preSig = it.pre_sig,
                                predPre = it.pred_pre,
                                fluRt = it.flu_rt,
                                accTrdeQty = it.acc_trde_qty,
                                netprpsAmt = it.netprps_amt,
                            )
                        )
                    }
                }
            }
        }

        return InvestorListRes(investorList = investorList)
    }

    // ─── 소켓 스트리밍 ────────────────────────────────────────────────────────

    fun investorStream(
        req: InvestorStreamReq
    ) {
        stockSocketClient.stockListStream(
            req = KiwoomStockStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    KiwoomStockStream(
                        item = req.items,
                        type = listOf("0B")
                    )
                )
            )
        )
    }
}
