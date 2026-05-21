package com.example.investfeed.kiwoom.order.client

import com.example.investfeed.kiwoom.annotation.KiwoomMockToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.DepositException
import com.example.investfeed.kiwoom.exception.HoldingListException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.RealizedPnlException
import com.example.investfeed.kiwoom.exception.TradeFillsException
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomDepositReq
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomHoldingReq
import com.example.investfeed.kiwoom.holding.dto.res.KiwoomDepositRes
import com.example.investfeed.kiwoom.holding.dto.res.KiwoomHoldingRes
import com.example.investfeed.kiwoom.order.dto.req.KiwoomTradeFillsReq
import com.example.investfeed.kiwoom.order.dto.res.KiwoomTradeFillsRes
import com.example.investfeed.kiwoom.realizedpnl.dto.req.KiwoomRealizedPnlReq
import com.example.investfeed.kiwoom.realizedpnl.dto.res.KiwoomRealizedPnlRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

/**
 * 키움 모의투자 계좌 조회 클라이언트 — 예수금/보유잔고/실현손익.
 *
 * 실거래 HoldingClient/RealizedPnlClient 와 **api-id·요청/응답 DTO 동일, base URL 만 모의 도메인**.
 * 토큰도 **모의 전용(@KiwoomMockToken / kiwoom.mock-appkey)** — 실거래와 redis 키·자격증명 분리.
 * 실거래 클라이언트는 무수정(모의 경로 격리). 기존 DTO 전부 재사용.
 * (계좌번호조회 ka00001 은 주문/조회가 토큰 계좌로 동작해 파이프라인상 불필요 → 의도적 생략.)
 */
@Service
class MockAccountClient(
    @param:Value("\${kiwoom.mock-url}")
    private val MOCK_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}

    @KiwoomMockToken
    fun deposit(req: KiwoomDepositReq): KiwoomDepositRes {
        val accessToken = authClient.getCurrentAccessTokenMock()
        try {
            val res = kiwoomWebClient.post()
                .uri("$MOCK_URL/api/dostk/acnt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "kt00001")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomDepositRes>()
                .block()

            if (res?.return_code != 0) {
                throw DepositException()
            }
            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: DepositException) {
            throw e
        } catch (e: Exception) {
            log.warn { "mock deposit Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    @KiwoomMockToken
    fun holdingList(req: KiwoomHoldingReq): KiwoomHoldingRes? {
        val accessToken = authClient.getCurrentAccessTokenMock()
        try {
            val res = kiwoomWebClient.post()
                .uri("$MOCK_URL/api/dostk/acnt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "kt00018")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomHoldingRes>()
                .block()

            if (res?.return_code != 0) {
                throw HoldingListException()
            }
            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: HoldingListException) {
            throw e
        } catch (e: Exception) {
            log.warn { "mock holdingList Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    @KiwoomMockToken
    fun realizedPnl(req: KiwoomRealizedPnlReq): KiwoomRealizedPnlRes? {
        val accessToken = authClient.getCurrentAccessTokenMock()
        try {
            val res = kiwoomWebClient.post()
                .uri("$MOCK_URL/api/dostk/acnt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10074")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomRealizedPnlRes>()
                .block()

            if (res?.return_code != 0) {
                throw RealizedPnlException()
            }
            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: RealizedPnlException) {
            throw e
        } catch (e: Exception) {
            log.warn { "mock realizedPnl Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    /**
     * 계좌별 주문체결내역 상세 — kt00007. 모의 도메인. qry_tp=4(체결내역만)로 조회 시
     * 체결된 주문만 응답. 본 메서드는 호출자가 qry_tp 등 ord_dt 외 파라미터를 req 에 직접 세팅.
     */
    @KiwoomMockToken
    fun tradeFills(req: KiwoomTradeFillsReq): KiwoomTradeFillsRes? {
        val accessToken = authClient.getCurrentAccessTokenMock()
        try {
            val res = kiwoomWebClient.post()
                .uri("$MOCK_URL/api/dostk/acnt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "kt00007")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomTradeFillsRes>()
                .block()

            if (res?.return_code != 0) {
                throw TradeFillsException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: TradeFillsException) {
            throw e
        } catch (e: Exception) {
            log.warn { "mock tradeFills Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }
}
