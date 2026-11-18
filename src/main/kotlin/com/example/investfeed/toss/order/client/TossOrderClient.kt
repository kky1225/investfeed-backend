package com.example.investfeed.toss.order.client

import com.example.investfeed.common.exception.ExternalApiFailureException
import com.example.investfeed.common.util.apiFailure
import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.toss.annotation.TossToken
import com.example.investfeed.toss.auth.service.TossAuthClient
import com.example.investfeed.toss.exception.TossApiException
import com.example.investfeed.toss.exception.TossOrderListException
import com.example.investfeed.toss.order.dto.res.TossOrder
import com.example.investfeed.toss.order.dto.res.TossOrderListRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

@Service
class TossOrderClient(
    @param:Value("\${toss.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("tossWebClient")
    private val tossWebClient: WebClient,
    private val tossAuthClient: TossAuthClient,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val PAGE_SIZE = 100   // 한 번에 받는 최대 건수
        // 이어 받는 횟수의 한도. 넘으면 받은 데까지만 돌려준다
        private const val MAX_PAGES = 10
    }

    /**
     * 한 종목의 종료된 주문 — GET /api/v1/orders?status=CLOSED. from~to 는 주문 시각(한국 날짜) 기준이고,
     * 쪽(cursor)을 끝까지 이어 받는다. 취소·거부된 주문도 섞여 오므로 체결 여부는 부르는 쪽이 가린다.
     */
    @TossToken
    suspend fun closedOrders(accountSeq: Long, symbol: String, from: LocalDate, to: LocalDate): List<TossOrder> {
        val accessToken = tossAuthClient.getCurrentAccessToken()

        try {
            val orders = mutableListOf<TossOrder>()
            var cursor: String? = null

            repeat(MAX_PAGES) {
                val page = tossWebClient.get()
                    .uri("$DEFAULT_URL/api/v1/orders") { builder ->
                        builder.queryParam("status", "CLOSED")
                            .queryParam("symbol", symbol)
                            .queryParam("from", from)
                            .queryParam("to", to)
                            .queryParam("limit", PAGE_SIZE)
                        // 커서는 값에 +, / 같은 글자가 있어도 그대로 전해지게 변수로 넣어 인코딩한다
                        cursor?.let { builder.queryParam("cursor", "{cursor}").build(it) } ?: builder.build()
                    }
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("X-Tossinvest-Account", accountSeq.toString())
                    .retrieve()
                    .onStatus({ it.isError }, { res -> res.logHttpError("토스"); throw TossApiException() })
                    .awaitBodyOrNull<TossOrderListRes>()
                    ?.result
                    ?: run {
                        log.error { "토스 API 응답 오류: api=closedOrders" }
                        throw TossOrderListException()
                    }

                orders += page.orders.orEmpty()
                cursor = page.nextCursor?.takeIf { page.hasNext == true && it.isNotBlank() }
                if (cursor == null) return orders
            }

            log.warn { "토스 주문 내역이 한도($MAX_PAGES 쪽)를 넘어 앞쪽만 받았습니다: api=closedOrders" }
            return orders
        } catch (e: TossApiException) {
            throw e
        } catch (e: TossOrderListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "toss closedOrders Error" }
            throw ExternalApiFailureException(e)
        }
    }
}
