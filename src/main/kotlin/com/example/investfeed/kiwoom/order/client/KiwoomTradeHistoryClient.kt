package com.example.investfeed.kiwoom.order.client

import com.example.investfeed.common.exception.ExternalApiFailureException
import com.example.investfeed.common.util.apiFailure
import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.TradeHistoryException
import com.example.investfeed.kiwoom.order.dto.req.KiwoomTradeHistoryReq
import com.example.investfeed.kiwoom.order.dto.res.KiwoomTradeHistoryItem
import com.example.investfeed.kiwoom.order.dto.res.KiwoomTradeHistoryRes
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import kotlin.coroutines.cancellation.CancellationException

/** 회원 본인 계좌의 거래내역 (실계좌). 모의투자의 체결 조회는 MockAccountClient 가 맡는다. */
@Service
class KiwoomTradeHistoryClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        // 한 기간을 이어 받는 횟수의 한도. 넘으면 받은 데까지만 돌려준다
        private const val MAX_PAGES = 10
    }

    /**
     * 위탁종합거래내역 — kt00015. 한 기간의 국내 주식 매매를 연속 조회(cont-yn·next-key)로 끝까지 받는다.
     * 내역이 없으면 빈 목록(return_code 0, "관련자료가없습니다").
     */
    @KiwoomToken
    suspend fun tradeHistory(req: KiwoomTradeHistoryReq): List<KiwoomTradeHistoryItem> {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val items = mutableListOf<KiwoomTradeHistoryItem>()
            var nextKey: String? = null

            repeat(MAX_PAGES) {
                val entity = kiwoomWebClient.post()
                    .uri("$DEFAULT_URL/api/dostk/acnt")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "kt00015")
                    .header("cont-yn", if (nextKey == null) "N" else "Y")
                    .header("next-key", nextKey.orEmpty())
                    .bodyValue(req)
                    .retrieve()
                    .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                    .toEntity<KiwoomTradeHistoryRes>()
                    .awaitSingleOrNull()

                val body = entity?.body
                if (body?.return_code != 0) {
                    // 개인 계좌 조회라 요청 값(종목·기간)은 남기지 않는다
                    log.error { "키움 API 응답 오류: api=tradeHistory, return_code=${body?.return_code}, return_msg=${body?.return_msg}" }
                    throw TradeHistoryException()
                }

                items += body.trst_ovrl_trde_prps_array.orEmpty()
                nextKey = entity.headers.getFirst("next-key")
                    ?.takeIf { entity.headers.getFirst("cont-yn") == "Y" && it.isNotBlank() }
                if (nextKey == null) return items
            }

            log.warn { "키움 거래내역이 한도($MAX_PAGES 쪽)를 넘어 앞쪽만 받았습니다: api=tradeHistory" }
            return items
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: TradeHistoryException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "tradeHistory Error" }
            throw ExternalApiFailureException(e)
        }
    }
}
