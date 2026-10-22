package com.example.investfeed.toss.market.client

import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.toss.annotation.TossToken
import com.example.investfeed.toss.auth.service.TossAuthClient
import com.example.investfeed.toss.exception.TossApiException
import com.example.investfeed.toss.exception.TossCandleException
import com.example.investfeed.toss.exception.TossMarketCalendarException
import com.example.investfeed.toss.market.dto.res.TossCandleRes
import com.example.investfeed.toss.market.dto.res.TossUsMarketCalendarRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import kotlin.coroutines.cancellation.CancellationException

@Service
class TossMarketClient(
    @param:Value("\${toss.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("tossWebClient")
    private val tossWebClient: WebClient,
    private val tossAuthClient: TossAuthClient,
) {
    private val log = KotlinLogging.logger {}

    @TossToken
    suspend fun dailyCandles(symbol: String, count: Int): TossCandleRes {
        val accessToken = tossAuthClient.getCurrentAccessToken()

        try {
            val res = tossWebClient.get()
                .uri("$DEFAULT_URL/api/v1/candles?symbol={symbol}&interval=1d&count={count}", symbol, count)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("토스"); throw TossApiException() })
                .awaitBodyOrNull<TossCandleRes>()

            if (res?.result == null) {
                log.error { "토스 API 응답 오류: api=dailyCandles, symbol=$symbol" }
                throw TossCandleException()
            }
            return res
        } catch (e: TossApiException) {
            throw e
        } catch (e: TossCandleException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "toss dailyCandles Error: symbol=$symbol, ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    @TossToken
    suspend fun usMarketCalendar(): TossUsMarketCalendarRes {
        val accessToken = tossAuthClient.getCurrentAccessToken()

        try {
            val res = tossWebClient.get()
                .uri("$DEFAULT_URL/api/v1/market-calendar/US")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("토스"); throw TossApiException() })
                .awaitBodyOrNull<TossUsMarketCalendarRes>()

            if (res?.result == null) {
                log.error { "토스 API 응답 오류: api=usMarketCalendar" }
                throw TossMarketCalendarException()
            }
            return res
        } catch (e: TossApiException) {
            throw e
        } catch (e: TossMarketCalendarException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "toss usMarketCalendar Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }
}
