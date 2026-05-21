package com.example.investfeed.kiwoom.order.client

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

/**
 * 키움 모의투자 주문 클라이언트 — 매수/매도/취소/미체결.
 *
 * 실거래와 API 구조 동일, **base URL 은 모의 도메인(`kiwoom.mock-url`)으로 스왑**.
 * 모의투자 도메인은 KRX 만 지원 → dmst_stex_tp/stex_tp 는 KRX 기준.
 * 토큰은 **모의 전용 토큰(@KiwoomMockToken / kiwoom.mock-appkey)** 사용 — 실거래 토큰과
 * redis 키·자격증명 분리. 실거래 클라이언트(HoldingClient 등)는 무수정.
 */
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
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomOrderRes>()
                .block()

            if (res?.return_code != 0) {
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
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomOrderRes>()
                .block()

            if (res?.return_code != 0) {
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
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomCancelOrderRes>()
                .block()

            if (res?.return_code != 0) {
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
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomPendingOrderRes>()
                .block()

            if (res?.return_code != 0) {
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