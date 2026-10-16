package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.crawler.NaverMarketIndexCrawler
import com.example.investfeed.domain.marketindex.service.MarketIndexService
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UsTreasuryService(
    private val crawler: NaverMarketIndexCrawler,
    private val marketIndexService: MarketIndexService,
    private val fallbackService: UsTreasuryFallbackService,
    private val usMarketCalendarService: UsMarketCalendarService,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        val SERIES_OF = mapOf(
            MarketIndexType.US_TREASURY_2Y to "US2Y",
            MarketIndexType.US_TREASURY_5Y to "US5Y",
            MarketIndexType.US_TREASURY_10Y to "US10Y",
            MarketIndexType.US_TREASURY_30Y to "US30Y",
        )
        const val SOURCE_NAVER = "NAVER"
        const val SOURCE_FRED = "FRED"
        private const val FRED_LOOKBACK_DAYS = 10L
    }

    data class Yield(val series: String, val tradeDate: LocalDate, val value: Double, val changeBp: Double?, val source: String)

    fun quote(type: MarketIndexType, usTradeDate: LocalDate): Yield? {
        val series = SERIES_OF[type] ?: return null
        val history = crawler.fetchBondDailyCloses(type)
        history.firstOrNull { it.tradeDate == usTradeDate }?.let { return Yield(series, it.tradeDate, it.close, it.changeBp, SOURCE_NAVER) }

        if (usMarketCalendarService.isUsBondHoliday(usTradeDate)) {
            return history.firstOrNull { it.tradeDate < usTradeDate }?.let { Yield(series, it.tradeDate, it.close, it.changeBp, SOURCE_NAVER) }
                ?: fred(series, usTradeDate.minusDays(1))
        }

        // 확정 일별 행은 17:05 ET 이후 생긴다 → 그 전(05:10 브리핑)엔 마감 직후 시세. 이력 조회가 성공했고 아직 그날 행이 없을 때만 쓴다
        val rowPending = history.isNotEmpty() && history.maxOf { it.tradeDate } < usTradeDate
        if (rowPending) {
            marketIndexService.getMarketIndex(type)?.takeIf { marketIndexService.staleAt(it) == null }?.let { res ->
                val value = TemplateFormat.parse(res.price) ?: return@let null
                return Yield(series, usTradeDate, value, TemplateFormat.parse(res.changeAmount)?.let { it * 100 }, SOURCE_NAVER)   // %p → bp
            }
        }

        log.warn { "미국 국채 $series $usTradeDate 네이버 값 없음 → FRED 대체" }
        return fred(series, usTradeDate)
    }

    // FRED 는 하루 늦게 갱신돼 보통 전 거래일 값이 나온다 (tradeDate 로 기준일이 드러남)
    private fun fred(series: String, upTo: LocalDate): Yield? {
        val obs = fallbackService.observations(series, upTo.minusDays(FRED_LOOKBACK_DAYS), upTo)
        val (date, value) = obs.lastOrNull() ?: return null
        val prev = obs.getOrNull(obs.size - 2)?.second
        return Yield(series, date, value, prev?.let { (value - it) * 100 }, SOURCE_FRED)
    }
}
