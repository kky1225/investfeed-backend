package com.example.investfeed.kiwoom.us.holding.client

import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.UsDepositException
import com.example.investfeed.kiwoom.exception.UsHoldingListException
import com.example.investfeed.kiwoom.us.holding.dto.req.KiwoomUsDepositReq
import com.example.investfeed.kiwoom.us.holding.dto.req.KiwoomUsHoldingReq
import com.example.investfeed.kiwoom.us.holding.dto.res.KiwoomUsDepositRes
import com.example.investfeed.kiwoom.us.holding.dto.res.KiwoomUsHoldingRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import kotlin.coroutines.cancellation.CancellationException
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class UsHoldingClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}
    private final val ACNT_URL = "/api/us/acnt"

    @KiwoomToken
    suspend fun usHoldingList(
        req: KiwoomUsHoldingReq
    ): KiwoomUsHoldingRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + ACNT_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ust21070")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomUsHoldingRes>()

            if (res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=usHoldingList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw UsHoldingListException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: UsHoldingListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "usHoldingList Error: ${e.message}" }

            throw RuntimeException(e.message)
        }
    }

    /** 해외주식 예수금(ust21110). 통화별 외화예수금·주문가능금액을 내려준다. */
    @KiwoomToken
    suspend fun usDeposit(
        req: KiwoomUsDepositReq
    ): KiwoomUsDepositRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + ACNT_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ust21110")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomUsDepositRes>()

            if (res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=usDeposit, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw UsDepositException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: UsDepositException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "usDeposit Error: ${e.message}" }

            throw RuntimeException(e.message)
        }
    }
}
