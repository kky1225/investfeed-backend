package com.example.investfeed.fred.client

import com.example.investfeed.fred.dto.res.FredReleaseDatesRes
import com.example.investfeed.fred.dto.res.FredSeriesRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class FredClient(
    @Qualifier("fredWebClient")
    private val fredWebClient: WebClient,
    @param:Value("\${fred.api-key}")
    private val apiKey: String,
) {
    private val log = KotlinLogging.logger {}

    /**
     * FRED 시리즈 관측값 조회
     * @param seriesId 시리즈 ID (예: FEDFUNDS, GDP, CPIAUCSL)
     * @param observationStart 시작일 (YYYY-MM-DD)
     * @param observationEnd 종료일 (YYYY-MM-DD, 선택)
     * @param frequency 주기 (선택: d, w, bw, m, q, sa, a)
     */
    fun getSeriesObservations(
        seriesId: String,
        observationStart: String? = null,
        observationEnd: String? = null,
        frequency: String? = null,
        realtimeStart: String? = null,
        realtimeEnd: String? = null,
        units: String? = null,
    ): FredSeriesRes? {
        repeat(3) { attempt ->
            try {
                return fredWebClient.get()
                    .uri { uriBuilder ->
                        uriBuilder.path("/fred/series/observations")
                            .queryParam("series_id", seriesId)
                            .queryParam("api_key", apiKey)
                            .queryParam("file_type", "json")
                        if (observationStart != null) uriBuilder.queryParam("observation_start", observationStart)
                        if (observationEnd != null) uriBuilder.queryParam("observation_end", observationEnd)
                        if (frequency != null) uriBuilder.queryParam("frequency", frequency)
                        if (realtimeStart != null) uriBuilder.queryParam("realtime_start", realtimeStart)
                        if (realtimeEnd != null) uriBuilder.queryParam("realtime_end", realtimeEnd)
                        if (units != null) uriBuilder.queryParam("units", units)
                        uriBuilder.build()
                    }
                    .retrieve()
                    .bodyToMono<FredSeriesRes>()
                    .block()
            } catch (e: Exception) {
                log.error { "FRED getSeriesObservations Error (seriesId=$seriesId, attempt=${attempt + 1}): ${e.message}" }
                if (attempt < 2) Thread.sleep(300L * (attempt + 1))
            }
        }
        return null
    }

    /**
     * 특정 release_id의 실제 발표일 조회 (/fred/release/dates)
     */
    fun getReleaseDatesByReleaseId(
        releaseId: Int,
        realtimeStart: String? = null,
        realtimeEnd: String? = null,
        sortOrder: String = "asc",
        includeReleaseDatesWithNoData: Boolean = false,
    ): FredReleaseDatesRes? {
        repeat(3) { attempt ->
            try {
                return fredWebClient.get()
                    .uri { uriBuilder ->
                        uriBuilder.path("/fred/release/dates")
                            .queryParam("release_id", releaseId)
                            .queryParam("api_key", apiKey)
                            .queryParam("file_type", "json")
                            .queryParam("include_release_dates_with_no_data", includeReleaseDatesWithNoData)
                            .queryParam("sort_order", sortOrder)
                            .queryParam("limit", 10000)
                        if (realtimeStart != null) uriBuilder.queryParam("realtime_start", realtimeStart)
                        if (realtimeEnd != null) uriBuilder.queryParam("realtime_end", realtimeEnd)
                        uriBuilder.build()
                    }
                    .retrieve()
                    .bodyToMono<FredReleaseDatesRes>()
                    .block()
            } catch (e: Exception) {
                log.error { "FRED getReleaseDatesByReleaseId Error (releaseId=$releaseId, attempt=${attempt + 1}): ${e.message}" }
                if (attempt < 2) Thread.sleep(300L * (attempt + 1))
            }
        }
        return null
    }
}
