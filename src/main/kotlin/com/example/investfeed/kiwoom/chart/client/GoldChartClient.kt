package com.example.investfeed.kiwoom.chart.client

import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.chart.dto.gold.req.KiwoomGoldChartDayReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.KiwoomGoldChartMinuteReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.KiwoomGoldChartMonthReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.KiwoomGoldChartWeekReq
import com.example.investfeed.kiwoom.chart.dto.gold.res.KiwoomGoldChartDayRes
import com.example.investfeed.kiwoom.chart.dto.gold.res.KiwoomGoldChartMinuteRes
import com.example.investfeed.kiwoom.chart.dto.gold.res.KiwoomGoldChartMonthRes
import com.example.investfeed.kiwoom.chart.dto.gold.res.KiwoomGoldChartWeekRes
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.GoldChartDayListException
import com.example.investfeed.kiwoom.exception.GoldChartMinuteListException
import com.example.investfeed.kiwoom.exception.GoldChartMonthListException
import com.example.investfeed.kiwoom.exception.GoldChartWeekListException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import kotlin.coroutines.cancellation.CancellationException
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Service
class GoldChartClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient
) {
    private val log = KotlinLogging.logger {}

    private val CHART_URL = "/api/dostk/chart"

    @KiwoomToken
    suspend fun goldChartMinuteList (
        req: KiwoomGoldChartMinuteReq
    ): KiwoomGoldChartMinuteRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + CHART_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50092")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomGoldChartMinuteRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=goldChartMinuteList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw GoldChartMinuteListException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldChartMinuteListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "goldChartMinuteList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    suspend fun goldChartDayList (
        req: KiwoomGoldChartDayReq
    ): KiwoomGoldChartDayRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + CHART_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50081")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomGoldChartDayRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=goldChartDayList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw GoldChartDayListException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldChartDayListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "goldChartDayList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    suspend fun goldChartWeekList (
        req: KiwoomGoldChartWeekReq
    ): KiwoomGoldChartWeekRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + CHART_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50082")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomGoldChartWeekRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=goldChartWeekList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw GoldChartWeekListException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldChartWeekListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "goldChartWeekList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    suspend fun goldChartMonthList (
        req: KiwoomGoldChartMonthReq
    ): KiwoomGoldChartMonthRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + CHART_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50083")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomGoldChartMonthRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=goldChartMonthList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw GoldChartMonthListException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldChartMonthListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "goldChartMonthList Error" }

            throw RuntimeException(e.message)
        }
    }
}