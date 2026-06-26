package com.example.investfeed.toss.exchangerate.client

import com.example.investfeed.toss.annotation.TossToken
import com.example.investfeed.toss.auth.service.TossAuthClient
import com.example.investfeed.toss.exception.TossApiException
import com.example.investfeed.toss.exception.TossExchangeRateException
import com.example.investfeed.toss.exchangerate.dto.res.TossExchangeRateRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class TossExchangeRateClient(
    @param:Value("\${toss.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("tossWebClient")
    private val tossWebClient: WebClient,
    private val tossAuthClient: TossAuthClient,
) {
    private val log = KotlinLogging.logger {}

    @TossToken
    fun getRate(baseCurrency: String, quoteCurrency: String): TossExchangeRateRes? {
        val accessToken = tossAuthClient.getCurrentAccessToken()

        try {
            val res = tossWebClient.get()
                .uri("$DEFAULT_URL/api/v1/exchange-rate?baseCurrency=$baseCurrency&quoteCurrency=$quoteCurrency")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .onStatus({ it.isError }, { throw TossApiException() })
                .bodyToMono<TossExchangeRateRes>()
                .block()

            if (res?.result == null) {
                throw TossExchangeRateException()
            }
            return res
        } catch (e: TossApiException) {
            throw e
        } catch (e: TossExchangeRateException) {
            throw e
        } catch (e: Exception) {
            log.warn { "toss getRate Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }
}
