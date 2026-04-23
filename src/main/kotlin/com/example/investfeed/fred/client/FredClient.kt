package com.example.investfeed.fred.client

import com.example.investfeed.fred.dto.res.FredReleaseDatesRes
import com.example.investfeed.fred.dto.res.FredSeriesRes
import com.example.investfeed.fred.exception.FredApiException
import com.example.investfeed.fred.exception.FredReleaseDatesException
import com.example.investfeed.fred.exception.FredSeriesObservationsException
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

    companion object {
        /**
         * FRED rate limit(120/min = 500ms per call) 완화를 위한 호출 간 최소 간격.
         * 500ms 로 분당 120 요청 한계에 맞춤 → rate limit 안전권.
         * 실측 50 콜 × 500ms = 약 25초 순수 대기 + API 응답 시간.
         */
        private const val THROTTLE_MS = 500L
    }

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
    ): FredSeriesRes {
        var lastError: Exception? = null
        repeat(3) { attempt ->
            try {
                val res = fredWebClient.get()
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
                    .onStatus({ it.isError }, { throw FredApiException() })
                    .bodyToMono<FredSeriesRes>()
                    .block()

                if (res?.observations == null) {
                    throw FredSeriesObservationsException()
                }

                Thread.sleep(THROTTLE_MS)
                return res
            } catch (e: FredApiException) {
                lastError = e
                log.warn { "getSeriesObservations attempt=${attempt + 1} 실패 (seriesId=$seriesId): ${e.message}" }
                if (attempt < 2) Thread.sleep(300L * (attempt + 1))
            } catch (e: FredSeriesObservationsException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                log.warn { "getSeriesObservations attempt=${attempt + 1} 실패 (seriesId=$seriesId): ${e.message}" }
                if (attempt < 2) Thread.sleep(300L * (attempt + 1))
            }
        }

        if (lastError is FredApiException) throw lastError as FredApiException
        throw RuntimeException(lastError?.message)
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
    ): FredReleaseDatesRes {
        var lastError: Exception? = null
        repeat(3) { attempt ->
            try {
                val res = fredWebClient.get()
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
                    .onStatus({ it.isError }, { throw FredApiException() })
                    .bodyToMono<FredReleaseDatesRes>()
                    .block()

                if (res?.release_dates == null) {
                    throw FredReleaseDatesException()
                }

                Thread.sleep(THROTTLE_MS)
                return res
            } catch (e: FredApiException) {
                lastError = e
                log.warn { "getReleaseDatesByReleaseId attempt=${attempt + 1} 실패 (releaseId=$releaseId): ${e.message}" }
                if (attempt < 2) Thread.sleep(300L * (attempt + 1))
            } catch (e: FredReleaseDatesException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                log.warn { "getReleaseDatesByReleaseId attempt=${attempt + 1} 실패 (releaseId=$releaseId): ${e.message}" }
                if (attempt < 2) Thread.sleep(300L * (attempt + 1))
            }
        }

        if (lastError is FredApiException) throw lastError as FredApiException
        throw RuntimeException(lastError?.message)
    }
}
