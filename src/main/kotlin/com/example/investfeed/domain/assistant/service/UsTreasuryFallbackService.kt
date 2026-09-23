package com.example.investfeed.domain.assistant.service

import com.example.investfeed.fred.client.FredClient
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UsTreasuryFallbackService(
    private val fredClient: FredClient,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        val FRED_SERIES = mapOf(
            "US2Y" to "DGS2",
            "US10Y" to "DGS10",
        )
    }

    fun observations(series: String, from: LocalDate, to: LocalDate): List<Pair<LocalDate, Double>> {
        val fredId = FRED_SERIES[series] ?: return emptyList()
        return try {
            fredClient.getSeriesObservations(fredId, observationStart = from.toString(), observationEnd = to.toString())
                .observations.orEmpty()
                .mapNotNull { o ->
                    val date = o.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@mapNotNull null
                    val value = o.value?.toDoubleOrNull() ?: return@mapNotNull null
                    date to value
                }
                .sortedBy { it.first }
        } catch (e: Exception) {
            log.error(e) { "FRED $fredId 조회 실패 ($from~$to)" }
            emptyList()
        }
    }
}
