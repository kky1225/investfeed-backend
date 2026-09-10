package com.example.investfeed.kiwoom.order.client

import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.kiwoom.annotation.KiwoomMockToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.BuyOrderException
import com.example.investfeed.kiwoom.exception.CancelOrderException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.PendingOrderException
import com.example.investfeed.kiwoom.exception.SellOrderException
import com.example.investfeed.kiwoom.order.dto.req.KiwoomCancelOrderReq
import com.example.investfeed.kiwoom.order.dto.req.KiwoomOrderReq
import com.example.investfeed.kiwoom.order.dto.req.KiwoomPendingOrderReq
import com.example.investfeed.kiwoom.order.dto.res.KiwoomCancelOrderRes
import com.example.investfeed.kiwoom.order.dto.res.KiwoomOrderRes
import com.example.investfeed.kiwoom.order.dto.res.KiwoomPendingOrderRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class KiwoomOrderClient(
    @param:Value("\${kiwoom.mock-url}")
    private val MOCK_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}

    @KiwoomMockToken
    fun placeBuyOrder(req: KiwoomOrderReq): KiwoomOrderRes {
        val accessToken = authClient.getCurrentAccessTokenMock()
        try {
            val res = kiwoomWebClient.post()
                .uri("$MOCK_URL/api/dostk/ordr")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "kt10000")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .bodyToMono<KiwoomOrderRes>()
                .block()

            if (res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=placeBuyOrder, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw BuyOrderException()
            }
            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: BuyOrderException) {
            throw e
        } catch (e: Exception) {
            log.warn { "placeBuyOrder Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    @KiwoomMockToken
    fun placeSellOrder(req: KiwoomOrderReq): KiwoomOrderRes {
        val accessToken = authClient.getCurrentAccessTokenMock()
        try {
            val res = kiwoomWebClient.post()
                .uri("$MOCK_URL/api/dostk/ordr")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "kt10001")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .bodyToMono<KiwoomOrderRes>()
                .block()

            if (res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=placeSellOrder, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw SellOrderException()
            }
            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: SellOrderException) {
            throw e
        } catch (e: Exception) {
            log.warn { "placeSellOrder Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    @KiwoomMockToken
    fun cancelOrder(req: KiwoomCancelOrderReq): KiwoomCancelOrderRes {
        val accessToken = authClient.getCurrentAccessTokenMock()
        try {
            val res = kiwoomWebClient.post()
                .uri("$MOCK_URL/api/dostk/ordr")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "kt10003")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .bodyToMono<KiwoomCancelOrderRes>()
                .block()

            if (res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=cancelOrder, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw CancelOrderException()
            }
            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: CancelOrderException) {
            throw e
        } catch (e: Exception) {
            log.warn { "cancelOrder Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    @KiwoomMockToken
    fun pendingOrders(req: KiwoomPendingOrderReq): KiwoomPendingOrderRes {
        val accessToken = authClient.getCurrentAccessTokenMock()
        try {
            val res = kiwoomWebClient.post()
                .uri("$MOCK_URL/api/dostk/acnt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10075")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .bodyToMono<KiwoomPendingOrderRes>()
                .block()

            if (res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=pendingOrders, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw PendingOrderException()
            }
            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: PendingOrderException) {
            throw e
        } catch (e: Exception) {
            log.warn { "pendingOrders Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }
}