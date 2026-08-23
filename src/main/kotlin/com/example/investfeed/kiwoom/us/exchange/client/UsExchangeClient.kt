package com.example.investfeed.kiwoom.us.exchange.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.*
import com.example.investfeed.kiwoom.us.exchange.dto.req.*
import com.example.investfeed.kiwoom.us.exchange.dto.res.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class UsExchangeClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}
    private final val EXCHANGE_URL = "/api/us/exchange"

    @KiwoomToken
    fun usExchangeRate(
        req: KiwoomUsExchangeRateReq
    ): KiwoomUsExchangeRateRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + EXCHANGE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ust31301")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomUsExchangeRateRes>()
                .block()

            if (res?.return_code != 0) {
                throw UsExchangeRateException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: UsExchangeRateException) {
            throw e
        } catch (e: Exception) {
            log.warn { "usExchangeRate Error : exchTp=${req.exch_tp}" }

            throw RuntimeException(e.message)
        }
    }
}
