package com.example.investfeed.kiwoom.holding.client

import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomDepositReq
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomHoldingReq
import com.example.investfeed.kiwoom.holding.dto.res.KiwoomDepositRes
import com.example.investfeed.kiwoom.holding.dto.res.KiwoomHoldingRes
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.DepositException
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
    ): KiwoomHoldingRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/acnt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "kt00018")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .bodyToMono<KiwoomHoldingRes>()
                .block()

            if (res == null || res.return_code != 0) {
                log.error { "키움 API 응답 오류: api=holdingList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw HoldingListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: HoldingListException) {
            throw e
        }catch (e: Exception) {
            log.warn { "holdingList Error: ${e.message}" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun deposit(
        req: KiwoomDepositReq
    ): KiwoomDepositRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/acnt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "kt00001")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .bodyToMono<KiwoomDepositRes>()
                .block()

            if (res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=deposit, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw DepositException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: DepositException) {
            throw e
        } catch (e: Exception) {
            log.warn { "deposit Error" }

            throw RuntimeException(e.message)
        }
    }
}
