package com.example.investfeed.domain.investor.service

import com.example.investfeed.common.util.MarketTimeUtil
import com.example.investfeed.domain.investor.dto.req.InvestorListReq
import com.example.investfeed.domain.investor.dto.req.InvestorStreamReq
import com.example.investfeed.domain.investor.dto.res.InvestorListItem
import com.example.investfeed.domain.investor.dto.res.InvestorListRes
import com.example.investfeed.global.constant.RedisKeyPrefix
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.price.client.PriceClient
import com.example.investfeed.kiwoom.investor.client.InvestorClient
import com.example.investfeed.kiwoom.investor.dto.req.KiwoomInvestorTradeDayReq
import com.example.investfeed.kiwoom.price.dto.req.KiwoomInvestorTradeOpenMarketReq
import com.example.investfeed.kiwoom.price.dto.res.KiwoomInvestorTradeOpenMarketItemList
import com.example.investfeed.kiwoom.socket.KiwoomStreamClient
import com.example.investfeed.kiwoom.socket.dto.StreamEntry
import com.example.investfeed.kiwoom.socket.dto.StreamMarket
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Collections.emptyList
import kotlinx.coroutines.runBlocking

@Service
class InvestorService(
    private val kiwoomStreamClient: KiwoomStreamClient,
    private val priceClient: PriceClient,
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val holidayService: HolidayService,
    private val investorClient: InvestorClient,
) {
    private val CACHE_PREFIX = RedisKeyPrefix.INVESTOR_CLOSE_MARKET.prefix

    companion object {
        private val ALL_COMBINATIONS = listOf("6" to "1", "6" to "2", "7" to "1", "7" to "2")
        const val MARKET_ALL = "000"
        const val ORGN_FOREIGN = "6"
        const val ORGN_INSTITUTION = "7"
        const val TRDE_BUY = "1"
        const val TRDE_SELL = "2"
        private val CLOSE_MARKETS = listOf("001", "101")   // ka10058 시장구분은 001·101 뿐(전체 없음) → 두 번 불러 합쳐 전체 순위를 만든다
        private const val LIST_SIZE = 100
        private val YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        private val SCHEDULE_RUN_START: LocalTime = MarketTimeUtil.KRX_TRADE_CLOSE  // 20:01
        private val SCHEDULE_RUN_END: LocalTime = LocalTime.of(21, 0)
        private val NEXT_DAY_REFRESH_TIME: LocalTime = SCHEDULE_RUN_START.plusMinutes(5)
    }

    internal fun ttlUntilNextInvestorUpdate(now: LocalDateTime): Duration {
        val nowTime = now.toLocalTime()
        val target: LocalDateTime =
            if (!nowTime.isBefore(SCHEDULE_RUN_START) && nowTime.isBefore(SCHEDULE_RUN_END)) {
                now.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
            } else {
                val baseDate: LocalDate =
                    if (!nowTime.isBefore(SCHEDULE_RUN_END)) now.toLocalDate()
                    else now.toLocalDate().minusDays(1)
                holidayService.nextTradingDay(baseDate).atTime(NEXT_DAY_REFRESH_TIME)
            }
        val duration = Duration.between(now, target)
        return if (duration.isNegative || duration.isZero) Duration.ofMinutes(1) else duration
    }

    /**
     * 투자자별 순위 (화면·비서 공용). 기준일과 잠정 여부를 함께 돌려준다.
     * - 거래일 08:00~20:00 (NXT 프리마켓부터): 장중 ka10063 (당일 잠정치, ETF 없음). 아직 당일 목록이 비어 있으면 직전 거래일 확정치
     * - 그 외(20:01 이후·개장 전·주말·휴장일): 직전에 끝난 거래일 확정치 ka10058 (ETF 포함). 결과는 항상 코스피+코스닥 전체
     *   확정치가 아직 없으면(20:01 직후 등) 장중 잠정치로 대신한다
     */
    fun listInvestors(req: InvestorListReq): InvestorListRes? {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val tradingToday = !holidayService.isHoliday(today)
        if (tradingToday && !now.toLocalTime().isBefore(MarketTimeUtil.NXT_OPEN) && !MarketTimeUtil.isKrxTradeClose(now.toLocalTime())) {
            openMarketProvisional(req, today)?.takeIf { !it.investorList.isNullOrEmpty() }?.let { return it }
            return getCloseMarketWithCache(req, holidayService.lastTradingDay(today.minusDays(1)))
        }
        val closedDay = if (tradingToday && MarketTimeUtil.isKrxTradeClose(now.toLocalTime())) today else holidayService.lastTradingDay(today.minusDays(1))
        return getCloseMarketWithCache(req, closedDay)
            ?: if (closedDay == today) openMarketProvisional(req, today) else null
    }

    private fun openMarketProvisional(req: InvestorListReq, today: LocalDate): InvestorListRes? =
        buildOpenMarketResult(req)?.copy(baseDate = today.toString(), provisional = true)

    private fun cacheKey(date: LocalDate, orgnTp: String, trdeTp: String) = "$CACHE_PREFIX$date:$orgnTp:$trdeTp"

    private fun getCloseMarketWithCache(req: InvestorListReq, date: LocalDate): InvestorListRes? {
        redisTemplate.opsForValue().get(cacheKey(date, req.orgnTp, req.trdeTp))?.let { cached ->
            return objectMapper.readValue(cached, InvestorListRes::class.java)
        }
        return refreshCloseMarketCache(date)?.get(req.orgnTp to req.trdeTp)
    }

    /**
     * date 확정치(ka10058)로 [외국인·기관] × [순매수·순매도] 4개 목록(코스피+코스닥 전체)을 만들어 캐시한다.
     * ka10058 은 시장구분에 전체가 없어(001·101 만) 한 목록당 두 번(총 8회) 불러 합친 뒤 다시 상위 100개를 고른다.
     * 확정치가 아직 없으면(4개 모두 빈 목록) 캐시하지 않고 null — 빈 화면이 다음 날까지 남지 않게
     */
    fun refreshCloseMarketCache(date: LocalDate = LocalDate.now()): Map<Pair<String, String>, InvestorListRes>? {
        val result = ALL_COMBINATIONS.associateWith { (orgnTp, trdeTp) ->
            InvestorListRes(rankItems(CLOSE_MARKETS.flatMap { m -> fetchClose(date, m, orgnTp, trdeTp) }, trdeTp), date.toString(), false)
        }
        if (result.values.all { it.investorList.isNullOrEmpty() }) return null

        val ttl = ttlUntilNextInvestorUpdate(LocalDateTime.now())
        result.forEach { (k, v) -> redisTemplate.opsForValue().set(cacheKey(date, k.first, k.second), objectMapper.writeValueAsString(v), ttl) }
        return result
    }

    /** 순매수는 금액 큰 순, 순매도는 금액 작은(음수 큰) 순으로 상위 100 */
    private fun rankItems(items: List<InvestorListItem>, trdeTp: String): List<InvestorListItem> {
        fun amt(i: InvestorListItem) = i.netprpsAmt?.toLongOrNull() ?: 0L
        return (if (trdeTp == TRDE_BUY) items.sortedByDescending { amt(it) } else items.sortedBy { amt(it) }).take(LIST_SIZE)
    }

    private fun fetchClose(date: LocalDate, mrktTp: String, orgnTp: String, trdeTp: String): List<InvestorListItem> {
        val dt = date.format(YYYYMMDD)
        val res = runBlocking {
            investorClient.investorTradeDay(KiwoomInvestorTradeDayReq(
                strt_dt = dt, end_dt = dt,
                trde_tp = if (trdeTp == TRDE_BUY) "2" else "1",   // ka10058: 2=순매수, 1=순매도
                mrkt_tp = mrktTp,
                invsr_tp = if (orgnTp == ORGN_FOREIGN) "9000" else "9999",
                stex_tp = "3",
            ))
        }
        return res?.invsr_daly_trde_stk.orEmpty().map { r ->
            val cur = r.cur_prc?.replace(Regex("^[+-]"), "")
            val pred = r.pred_pre?.replace("--", "-")
            InvestorListItem(
                stkCd = r.stk_cd,
                stkNm = r.stk_nm,
                curPrc = cur,
                preSig = r.pre_sig,
                predPre = pred,
                fluRt = fluRt(cur, pred),
                accTrdeQty = r.dt_trde_qty,
                netprpsAmt = r.netslmt_amt?.replace("--", "-")?.removePrefix("+"),
                netprpsQty = r.netslmt_qty?.replace("--", "-")?.removePrefix("+"),
            )
        }
    }

    /** 등락률 "+6.26" 형식 (ka10066 flu_rt 와 같은 모양). 현재가·전일대비로 계산 */
    private fun fluRt(cur: String?, pred: String?): String? {
        val c = cur?.toDoubleOrNull() ?: return null
        val d = pred?.replace("+", "")?.toDoubleOrNull() ?: return null
        val prev = c - d
        if (prev <= 0) return null
        return String.format("%+.2f", d / prev * 100)
    }

    private fun buildOpenMarketResult(req: InvestorListReq): InvestorListRes? {
        val investorList: MutableList<InvestorListItem> = mutableListOf()
        var openResult: MutableList<KiwoomInvestorTradeOpenMarketItemList>

        when (req.orgnTp) {
            "6" -> {
                val kiwoomInvestorTradeDailyRes1 = runBlocking { priceClient.investorTradeOpenMarket(
                    req = KiwoomInvestorTradeOpenMarketReq(
                        mrkt_tp = MARKET_ALL,
                        amt_qty_tp = "1",
                        invsr = req.orgnTp,
                        frgn_all = "0",
                        smtm_netprps_tp = "1",
                        stex_tp = "3",
                    )
                ) }

                val kiwoomInvestorTradeDailyRes2 = runBlocking { priceClient.investorTradeOpenMarket(
                    req = KiwoomInvestorTradeOpenMarketReq(
                        mrkt_tp = MARKET_ALL,
                        amt_qty_tp = "1",
                        invsr = req.orgnTp,
                        frgn_all = "1",
                        smtm_netprps_tp = "1",
                        stex_tp = "3",
                    )
                ) }

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
                val kiwoomInvestorTradeDailyRes = runBlocking { priceClient.investorTradeOpenMarket(
                    req = KiwoomInvestorTradeOpenMarketReq(
                        mrkt_tp = MARKET_ALL,
                        amt_qty_tp = "1",
                        invsr = req.orgnTp,
                        frgn_all = "0",
                        smtm_netprps_tp = "1",
                        stex_tp = "3",
                    )
                ) }

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

    fun streamInvestors(
        req: InvestorStreamReq
    ) {
        kiwoomStreamClient.register(
            StreamEntry(
                market = StreamMarket.NXT,
                items = req.items,
                types = listOf("0B")
            )
        )
    }
}
