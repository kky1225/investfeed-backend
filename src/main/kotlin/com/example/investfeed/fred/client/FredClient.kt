package com.example.investfeed.fred.client

import com.example.investfeed.common.exception.ExternalApiFailureException
import com.example.investfeed.common.exception.InvestFeedException
import com.example.investfeed.common.util.apiFailure
import com.example.investfeed.fred.dto.res.FredReleaseDatesRes
import com.example.investfeed.fred.dto.res.FredSeriesRes
import com.example.investfeed.fred.dto.res.FredVintageDatesRes
import com.example.investfeed.fred.exception.FredApiException
import com.example.investfeed.fred.exception.FredHtmlResponseException
import com.example.investfeed.fred.exception.FredReleaseDatesException
import com.example.investfeed.fred.exception.FredSeriesObservationsException
import com.example.investfeed.fred.exception.FredVintageDatesException
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

/**
 * 일시 장애 재시도는 FredRetryFilter 가 한다. 여기서는 마지막 결과만 보고 실패 1건에 error 1건을 남긴다.
 * 로그에 URL 을 남기지 않는다 (URL 에 API 키가 들어 있어 logHttpError 를 쓰지 않는다).
 */
@Service
class FredClient(
    @Qualifier("fredWebClient")
    private val fredWebClient: WebClient,
    @param:Value("\${fred.api-key}")
    private val apiKey: String,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val HTML_TITLE = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val SCRIPT_STYLE = Regex("<(script|style)[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val TAG = Regex("<[^>]+>")
        private val WS = Regex("\\s+")
        private val DIAG_HEADERS = setOf("server", "content-type", "content-length", "date", "retry-after", "location", "cache-control")
    }

    /**
     * 점검·장애·차단 때 FRED(앞단 Akamai)는 200 OK 로 JSON 대신 HTML 페이지를 준다(2026-10-22 00:00~13:32).
     * 그대로 역직렬화하면 "200 OK … Content type 'text/html' not supported" 로만 남아 원인이 안 보이므로
     * 본문을 먼저 확인하고, 사유를 알 수 있게 헤더·제목·본문 텍스트를 error 에, 원문 HTML 은 파일 로그(warn)에 남긴다
     */
    private inline fun <reified T> parseJson(api: String, res: ResponseEntity<String>?): T? {
        val body = res?.body
        if (body.isNullOrBlank()) return null
        if (body.trimStart().startsWith("<")) {
            val headers = res.headers.toSingleValueMap()
                .filterKeys { name -> name.lowercase().let { it in DIAG_HEADERS || it.startsWith("x-") || "akamai" in it } }
                .entries.joinToString(", ") { "${it.key}=${it.value}" }
            val title = HTML_TITLE.find(body)?.groupValues?.get(1)?.replace(WS, " ")?.trim().orEmpty()
            val text = body.replace(SCRIPT_STYLE, " ").replace(TAG, " ").replace(WS, " ").trim().take(500)
            val summary = "status=${res.statusCode}, headers=[$headers], title=$title, text=$text"
            log.warn { "FRED HTML 응답 원문: api=$api, body=${body.take(3000)}" }
            log.error { "FRED 가 JSON 대신 HTML 을 반환: api=$api, $summary" }
            throw FredHtmlResponseException(summary)
        }
        return objectMapper.readValue(body, T::class.java)
    }

    /** HTTP 오류 응답. 재시도는 필터가 끝냈으므로 여기서 error 1건 */
    private fun WebClient.ResponseSpec.onFredError(api: String): WebClient.ResponseSpec =
        onStatus({ it.isError }, { res ->
            res.bodyToMono(String::class.java).defaultIfEmpty("(no body)").map { body ->
                log.error { "FRED API HTTP 오류: api=$api, status=${res.statusCode()}, body=${body.take(500)}" }
                FredApiException()
            }
        })

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
                .onFredError("getSeriesObservations")
                .toEntity(String::class.java)
                .block()
                .let { parseJson<FredSeriesRes>("getSeriesObservations", it) }

            if (res?.observations == null) {
                log.error { "FRED API 응답 오류: api=getSeriesObservations, seriesId=$seriesId" }
                throw FredSeriesObservationsException()
            }

            return res
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e, secretInUrl = true) { "getSeriesObservations 실패 (seriesId=$seriesId)" }
            throw ExternalApiFailureException()
        }
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
                .onFredError("getReleaseDatesByReleaseId")
                .toEntity(String::class.java)
                .block()
                .let { parseJson<FredReleaseDatesRes>("getReleaseDatesByReleaseId", it) }

            if (res?.release_dates == null) {
                log.error { "FRED API 응답 오류: api=getReleaseDatesByReleaseId, releaseId=$releaseId" }
                throw FredReleaseDatesException()
            }

            return res
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e, secretInUrl = true) { "getReleaseDatesByReleaseId 실패 (releaseId=$releaseId)" }
            throw ExternalApiFailureException()
        }
    }

    fun getSeriesVintageDates(
        seriesId: String,
        realtimeStart: String? = null,
        realtimeEnd: String? = null,
    ): FredVintageDatesRes {
        try {
            val res = fredWebClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("/fred/series/vintagedates")
                        .queryParam("series_id", seriesId)
                        .queryParam("api_key", apiKey)
                        .queryParam("file_type", "json")
                        .queryParam("limit", 10000)
                    if (realtimeStart != null) uriBuilder.queryParam("realtime_start", realtimeStart)
                    if (realtimeEnd != null) uriBuilder.queryParam("realtime_end", realtimeEnd)
                    uriBuilder.build()
                }
                .retrieve()
                .onFredError("getSeriesVintageDates")
                .toEntity(String::class.java)
                .block()
                .let { parseJson<FredVintageDatesRes>("getSeriesVintageDates", it) }

            if (res?.vintage_dates == null) {
                log.error { "FRED API 응답 오류: api=getSeriesVintageDates, seriesId=$seriesId" }
                throw FredVintageDatesException()
            }

            return res
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e, secretInUrl = true) { "getSeriesVintageDates 실패 (seriesId=$seriesId)" }
            throw ExternalApiFailureException()
        }
    }
}
