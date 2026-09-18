package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.entity.AssistantUsIndexDaily
import com.example.investfeed.domain.assistant.repository.AssistantUsIndexDailyRepository
import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.service.MarketIndexService
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.math.abs

@Service
class UsIndexDailyService(
    private val repository: AssistantUsIndexDailyRepository,
    private val marketIndexService: MarketIndexService,
    private val fallbackService: UsTreasuryFallbackService,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        val SERIES_OF = mapOf(
            MarketIndexType.NASDAQ to "NASDAQ",
            MarketIndexType.SP500 to "SPX",
            MarketIndexType.DOW to "DOW",
            MarketIndexType.PHILADELPHIA_SEMICONDUCTOR to "SOX",
            MarketIndexType.VIX to "VIX",
            MarketIndexType.US_TREASURY_2Y to "US2Y",
            MarketIndexType.US_TREASURY_10Y to "US10Y",
        )
        val YIELD_SERIES = setOf("US2Y", "US10Y")
        const val SOURCE_NAVER = "NAVER"
        const val SOURCE_FRED = "FRED"
        const val BACKFILL_DAYS = 30L
        const val STREAK_MIN = 3
    }

    data class Daily(val series: String, val tradeDate: LocalDate, val value: Double, val change: Double?, val source: String)

    @Transactional
    fun captureFromRedis(tradeDate: LocalDate): List<String> {
        val saved = SERIES_OF.mapNotNull { (type, series) ->
            val res = marketIndexService.getMarketIndex(type) ?: return@mapNotNull null
            val value = res.price.replace(",", "").toDoubleOrNull() ?: return@mapNotNull null
            val change = if (series in YIELD_SERIES) {
                res.changeAmount.replace(",", "").toDoubleOrNull()?.let { it * 100 }   // %p → bp
            } else {
                res.changeRate.removeSuffix("%").replace(",", "").toDoubleOrNull()
            }
            upsert(series, tradeDate, value, change, SOURCE_NAVER)
            series
        }
        val missing = SERIES_OF.values - saved.toSet()
        if (missing.isNotEmpty()) log.warn { "미국 일별 저장 결측 ($tradeDate): $missing → FRED 백업 시도" }
        missing.forEach { fillFromFred(it, tradeDate) }
        return saved
    }

    @Transactional
    fun backfillIfEmpty(upTo: LocalDate) {
        UsTreasuryFallbackService.FRED_SERIES.keys.forEach { series ->
            if (repository.findTop30BySeriesOrderByTradeDateDesc(series).isNotEmpty()) return@forEach
            val obs = fallbackService.observations(series, upTo.minusDays(BACKFILL_DAYS), upTo)
            obs.forEachIndexed { i, (date, value) ->
                val prev = obs.getOrNull(i - 1)?.second
                upsert(series, date, value, changeOf(series, value, prev), SOURCE_FRED)
            }
        }
    }

    fun findByDate(series: String, tradeDate: LocalDate): Daily? = repository.findBySeriesAndTradeDate(series, tradeDate)?.toDaily()

    fun latestOnOrBefore(series: String, tradeDate: LocalDate): Daily? =
        repository.findTop30BySeriesOrderByTradeDateDesc(series).firstOrNull { !it.tradeDate.isAfter(tradeDate) }?.toDaily()

    private fun fillFromFred(series: String, tradeDate: LocalDate) {
        val obs = fallbackService.observations(series, tradeDate.minusDays(10), tradeDate)
        val idx = obs.indexOfLast { it.first <= tradeDate }
        if (idx < 0) return
        val (date, value) = obs[idx]
        if (date != tradeDate) {
            log.warn { "FRED $series: $tradeDate 값 없음, 최근 $date 값으로 대체하지 않음" }
            return
        }
        upsert(series, date, value, changeOf(series, value, obs.getOrNull(idx - 1)?.second), SOURCE_FRED)
    }

    private fun changeOf(series: String, value: Double, prev: Double?): Double? {
        prev ?: return null
        return if (series in YIELD_SERIES) (value - prev) * 100 else if (abs(prev) > 0) (value / prev - 1) * 100 else null
    }

    private fun upsert(series: String, tradeDate: LocalDate, value: Double, change: Double?, source: String) {
        val existing = repository.findBySeriesAndTradeDate(series, tradeDate)
        if (existing == null) {
            repository.save(AssistantUsIndexDaily(series = series, tradeDate = tradeDate, value = value, change = change, source = source))
        } else if (existing.source != SOURCE_NAVER || source == SOURCE_NAVER) {
            existing.value = value
            existing.change = change
            existing.source = source
            repository.save(existing)
        }
    }

    private fun AssistantUsIndexDaily.toDaily() = Daily(series, tradeDate, value, change, source)
}
