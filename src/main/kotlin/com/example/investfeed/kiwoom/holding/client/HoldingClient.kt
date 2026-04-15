package com.example.investfeed.kiwoom.holding.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomDepositReq
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomHoldingReq
import com.example.investfeed.kiwoom.holding.dto.res.KiwoomDepositRes
import com.example.investfeed.kiwoom.holding.dto.res.KiwoomHoldingRes
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.HoldingListException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class HoldingClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}

    @KiwoomToken
    fun holdingList(
        req: KiwoomHoldingReq
    ): KiwoomHoldingRes? {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/acnt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "kt00018")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomHoldingRes>()
                .block()

            if(res?.return_code != 0) {
                throw HoldingListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: HoldingListException) {
            throw e
        }catch (e: Exception) {
            log.error { "holdingList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun deposit(
        req: KiwoomDepositReq
    ): KiwoomDepositRes? {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/acnt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "kt00001")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomDepositRes>()
                .block()

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: Exception) {
            log.error { "deposit Error: ${e.message}" }
            return null
        }
    }
}
