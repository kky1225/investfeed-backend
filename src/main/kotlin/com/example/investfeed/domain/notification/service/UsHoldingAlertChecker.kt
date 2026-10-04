package com.example.investfeed.domain.notification.service

import com.example.investfeed.domain.assistant.dto.factsheet.HoldingAlertHit
import com.example.investfeed.domain.assistant.service.UsMarketCalendarService
import com.example.investfeed.domain.holding.entity.MemberHolding
import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.Direction
import com.example.investfeed.domain.us.stock.repository.UsStockMasterRepository
import com.example.investfeed.kiwoom.us.stock.client.UsStockClient
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockInfoReq
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Service
class UsHoldingAlertChecker(
    private val usStockClient: UsStockClient,
    private val usStockMasterRepository: UsStockMasterRepository,
    private val usMarketCalendarService: UsMarketCalendarService,
    private val judge: PriceAlertJudge,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        val US_STOCK_THRESHOLDS = listOf(5.0, 10.0, 15.0, 20.0, 30.0)
        const val US_SUFFIX = "_US"
        private val US_OPEN_ET: LocalTime = LocalTime.of(9, 30)
        private val YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    fun check(usHoldings: List<MemberHolding>, hits: MutableList<HoldingAlertHit>, now: LocalDateTime) {
        if (usHoldings.isEmpty()) return

        val ny = now.atZone(UsMarketCalendarService.KST).withZoneSameInstant(UsMarketCalendarService.NY)
        val usDate = ny.toLocalDate()
        val nyTime = ny.toLocalTime()
        if (!usMarketCalendarService.isUsTradingDay(usDate) || nyTime.isBefore(US_OPEN_ET) || nyTime.isAfter(usMarketCalendarService.closeTimeEt(usDate))) return

        val byTicker = usHoldings.groupBy { it.stkCd.removeSuffix(US_SUFFIX) }
        val stexByTicker = usStockMasterRepository.findByStkCdIn(byTicker.keys).groupBy { it.stkCd }.mapValues { (_, rows) -> rows.first().stexTp }
        val todayUs = usDate.format(YYYYMMDD)
        val seen = mutableSetOf<Pair<Long, String>>()

        byTicker.forEach { (ticker, holders) ->
            val stexTp = stexByTicker[ticker] ?: run { log.warn { "미국 보유 종목 거래소 구분 없음(us_stock_master): $ticker" }; return@forEach }
            val res = try {
                usStockClient.usStockInfo(KiwoomUsStockInfoReq(stex_tp = stexTp, stk_cd = ticker))
            } catch (e: Exception) {
                log.warn { "미국 보유 종목 시세 조회 실패: $ticker ${e.message}" }
                return@forEach
            }
            val base = price(res.base_close_pric) ?: return@forEach
            val high = price(res.high_pric) ?: return@forEach
            val low = price(res.low_pric) ?: return@forEach
            val curPrice = price(res.cur_prc)
            val curRate = curPrice?.let { (it - base) / base * 100 }
            val maxUpRt = (high - base) / base * 100
            val maxDownRt = (low - base) / base * 100
            val link = "/us-stock/detail/$stexTp/$ticker"

            holders.filter { seen.add(it.memberId to it.stkCd) }.forEach { h ->
                val t = AlertTarget(h.memberId, AssetType.STOCK, h.stkCd, h.stkNm, link, held = true, curRate = curRate, curPrice = curPrice)
                if (maxUpRt > 0) judge.judge(t, Direction.UP, maxUpRt, US_STOCK_THRESHOLDS, hits, usDate)
                if (maxDownRt < 0) judge.judge(t, Direction.DOWN, maxDownRt, US_STOCK_THRESHOLDS, hits, usDate)
                if (res.wk52_hgst_pric_dt == todayUs) judge.judge(t, Direction.HIGH_52W, high, PriceAlertJudge.SINGLE, hits, usDate)
                if (res.wk52_lwst_pric_dt == todayUs) judge.judge(t, Direction.LOW_52W, low, PriceAlertJudge.SINGLE, hits, usDate)
            }
        }
    }

    private fun price(s: String?): Double? = s?.toDoubleOrNull()?.let { abs(it) }?.takeIf { it > 0 }
}
