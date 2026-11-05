package com.example.investfeed.global.ratelimit

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.domain.monitoring.service.ApiCallCounterService
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * 키움 조회 API 의 HTTP 429 를 1회 재시도한다.
 *
 * 키움은 조회와 주문이 모두 POST 라서 api-id 접두어로 구분한다.
 * 목록에 없는 api-id(주문 포함)는 재시도하지 않는다.
 */
@Component
class KiwoomRateLimitRetryFilter(
    private val limiter: MinIntervalRateLimiter,
    private val policy: RateLimitPolicy,
    private val apiCallCounterService: ApiCallCounterService,
) : ExchangeFilterFunction {

    private val log = KotlinLogging.logger {}

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> =
        next.exchange(request).flatMap { response ->
            val apiId = request.headers().getFirst("api-id")
            if (response.statusCode().value() == TOO_MANY_REQUESTS && isRetryable(apiId)) {
                log.warn { "키움 429 수신 — ${RETRY_DELAY_MS}ms 후 1회 재시도: api=$apiId" }
                apiCallCounterService.recordRetry(ApiProvider.KIWOOM)
                apiCallCounterService.incrementAsync(ApiProvider.KIWOOM)
                response.releaseBody()
                    .then(Mono.delay(Duration.ofMillis(RETRY_DELAY_MS)))
                    .then(Mono.defer { resend(request, next) })
            } else {
                Mono.just(response)
            }
        }

    /** 재시도도 호출 간격 제한을 지키도록 슬롯을 다시 예약한다 */
    private fun resend(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        val delayMs = policy.resolve(request).maxOfOrNull { limiter.reserve(it.key, it.intervalMs) } ?: 0L
        return Mono.delay(Duration.ofMillis(delayMs)).then(Mono.defer { next.exchange(request) })
    }

    companion object {
        private const val TOO_MANY_REQUESTS = 429
        private const val RETRY_DELAY_MS = 1_000L
        private val RETRYABLE_API_ID_PREFIXES = listOf("ka", "usa", "kt00")

        internal fun isRetryable(apiId: String?): Boolean =
            apiId != null && RETRYABLE_API_ID_PREFIXES.any { apiId.startsWith(it) }
    }
}
