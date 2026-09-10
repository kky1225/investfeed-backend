package com.example.investfeed.toss.holding.client

import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.toss.annotation.TossToken
import com.example.investfeed.toss.auth.service.TossAuthClient
import com.example.investfeed.toss.exception.TossApiException
import com.example.investfeed.toss.holding.dto.res.TossBuyingPowerRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class TossBuyingPowerClient(
    @param:Value("\${toss.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("tossWebClient")
    private val tossWebClient: WebClient,
    private val tossAuthClient: TossAuthClient,
) {
    private val log = KotlinLogging.logger {}

    @TossToken
    fun getBuyingPower(accountSeq: Long, currency: String): TossBuyingPowerRes? {
        val accessToken = tossAuthClient.getCurrentAccessToken()

        try {
            return tossWebClient.get()
                .uri("$DEFAULT_URL/api/v1/buying-power?currency=$currency")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("X-Tossinvest-Account", accountSeq.toString())
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("토스"); throw TossApiException() })
                .bodyToMono<TossBuyingPowerRes>()
                .block()
        } catch (e: TossApiException) {
            throw e
        } catch (e: Exception) {
            log.warn { "toss getBuyingPower Error: currency=$currency, ${e.message}" }
            throw RuntimeException(e.message)
        }
    }
}
