package com.example.investfeed.fred.client

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
     * 본문을 먼저 확인하고, 사유를 알 수 있게 헤더·제목·본문 텍스트를 메시지에, 원문 HTML 은 파일 로그(warn)에 남긴다
     */
    private inline fun <reified T> parseJson(res: ResponseEntity<String>?): T? {
        val body = res?.body
        if (body.isNullOrBlank()) return null
        if (body.trimStart().startsWith("<")) {
            val headers = res.headers.toSingleValueMap()
                .filterKeys { name -> name.lowercase().let { it in DIAG_HEADERS || it.startsWith("x-") || "akamai" in it } }
                .entries.joinToString(", ") { "${it.key}=${it.value}" }
            val title = HTML_TITLE.find(body)?.groupValues?.get(1)?.replace(WS, " ")?.trim().orEmpty()
            val text = body.replace(SCRIPT_STYLE, " ").replace(TAG, " ").replace(WS, " ").trim().take(500)
            log.warn { "FRED HTML 응답 원문: status=${res.statusCode}, headers=[$headers], body=${body.take(3000)}" }
            throw FredHtmlResponseException("status=${res.statusCode}, headers=[$headers], title=$title, text=$text")
        }
        return objectMapper.readValue(body, T::class.java)
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
                    .onStatus({ it.isError }, { res ->
                        res.bodyToMono(String::class.java).defaultIfEmpty("(no body)").map { body ->
                            log.warn { "FRED API HTTP error: status=${res.statusCode()}, body=${body.take(500)}" }
                            FredApiException()
                        }
                    })
                    .toEntity(String::class.java)
                    .block()
                    .let { parseJson<FredSeriesRes>(it) }

                if (res?.observations == null) {
                    throw FredSeriesObservationsException()
                }

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
                    .onStatus({ it.isError }, { res ->
                        res.bodyToMono(String::class.java).defaultIfEmpty("(no body)").map { body ->
                            log.warn { "FRED API HTTP error: status=${res.statusCode()}, body=${body.take(500)}" }
                            FredApiException()
                        }
                    })
                    .toEntity(String::class.java)
                    .block()
                    .let { parseJson<FredReleaseDatesRes>(it) }

                if (res?.release_dates == null) {
                    throw FredReleaseDatesException()
                }

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

    fun getSeriesVintageDates(
        seriesId: String,
        realtimeStart: String? = null,
        realtimeEnd: String? = null,
    ): FredVintageDatesRes {
        var lastError: Exception? = null
        repeat(3) { attempt ->
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
                    .onStatus({ it.isError }, { res ->
                        res.bodyToMono(String::class.java).defaultIfEmpty("(no body)").map { body ->
                            log.warn { "FRED API HTTP error: status=${res.statusCode()}, body=${body.take(500)}" }
                            FredApiException()
                        }
                    })
                    .toEntity(String::class.java)
                    .block()
                    .let { parseJson<FredVintageDatesRes>(it) }

                if (res?.vintage_dates == null) {
                    throw FredVintageDatesException()
                }

                return res
            } catch (e: FredApiException) {
                lastError = e
                log.warn { "getSeriesVintageDates attempt=${attempt + 1} 실패 (seriesId=$seriesId): ${e.message}" }
                if (attempt < 2) Thread.sleep(300L * (attempt + 1))
            } catch (e: FredVintageDatesException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                log.warn { "getSeriesVintageDates attempt=${attempt + 1} 실패 (seriesId=$seriesId): ${e.message}" }
                if (attempt < 2) Thread.sleep(300L * (attempt + 1))
            }
        }

        if (lastError is FredApiException) throw lastError as FredApiException
        throw RuntimeException(lastError?.message)
    }
}
