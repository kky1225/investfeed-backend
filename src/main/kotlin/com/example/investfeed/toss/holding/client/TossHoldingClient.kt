package com.example.investfeed.toss.holding.client

import com.example.investfeed.toss.annotation.TossToken
import com.example.investfeed.toss.auth.service.TossAuthClient
import com.example.investfeed.toss.exception.TossApiException
import com.example.investfeed.toss.exception.TossHoldingListException
import com.example.investfeed.toss.holding.dto.res.TossHoldingRes
import com.example.investfeed.toss.holding.dto.res.TossHoldingResult
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class TossHoldingClient(
    @param:Value("\${toss.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("tossWebClient")
    private val tossWebClient: WebClient,
    private val tossAuthClient: TossAuthClient,
) {
    private val log = KotlinLogging.logger {}

    @TossToken
    fun getHoldings(accountSeq: Long): TossHoldingResult? {
        val accessToken = tossAuthClient.getCurrentAccessToken()

        try {
            val res = tossWebClient.get()
                .uri("$DEFAULT_URL/api/v1/holdings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("X-Tossinvest-Account", accountSeq.toString())
                .retrieve()
                .onStatus({ it.isError }, { throw TossApiException() })
                .bodyToMono<TossHoldingRes>()
                .block()

            return res?.result ?: throw TossHoldingListException()
        } catch (e: TossApiException) {
            throw e
        } catch (e: TossHoldingListException) {
            throw e
        } catch (e: Exception) {
            log.warn { "toss getHoldings Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }
}
