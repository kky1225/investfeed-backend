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
 * FRED 의 일시 장애(5xx·429·연결 실패)를 최대 2회 재시도한다. FRED 는 조회(GET)만 쓴다.
 *
 * 재시도 중간 실패는 warn 만 남기고, 마지막 결과는 그대로 넘긴다
 * → 마지막 실패의 error 는 ApiCallMetricsFilter(연결 실패)·FredClient(HTTP 오류)가 1건 남긴다.
 * 로그에 URL 을 남기지 않는다 (URL 에 API 키가 들어 있다).
 */
@Component
class FredRetryFilter(
    private val limiter: MinIntervalRateLimiter,
    private val policy: RateLimitPolicy,
    private val apiCallCounterService: ApiCallCounterService,
) : ExchangeFilterFunction {

    private val log = KotlinLogging.logger {}

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> = attempt(request, next, 0)

    private fun attempt(request: ClientRequest, next: ExchangeFunction, attemptNo: Int): Mono<ClientResponse> {
        val canRetry = attemptNo < MAX_RETRY
        val path = request.url().path
        return next.exchange(request)
            .flatMap { response ->
                val status = response.statusCode().value()
                if (canRetry && isRetryableStatus(status)) {
                    log.warn { "FRED $status 수신 — 재시도(${attemptNo + 1}/$MAX_RETRY): path=$path" }
                    response.releaseBody().then(retry(request, next, attemptNo))
                } else {
                    Mono.just(response)
                }
            }
            .onErrorResume({ canRetry }) { e ->
                log.warn { "FRED 연결 실패 — 재시도(${attemptNo + 1}/$MAX_RETRY): path=$path, ${e.javaClass.simpleName}: ${e.message}" }
                retry(request, next, attemptNo)
            }
    }

    /** 재시도도 호출 간격 제한을 지키도록 슬롯을 다시 예약한다 */
    private fun retry(request: ClientRequest, next: ExchangeFunction, attemptNo: Int): Mono<ClientResponse> {
        apiCallCounterService.recordRetry(ApiProvider.FRED)
        apiCallCounterService.incrementAsync(ApiProvider.FRED)
        return Mono.delay(Duration.ofMillis(BACKOFF_MS * (attemptNo + 1)))
            .then(Mono.defer {
                val delayMs = policy.resolve(request).maxOfOrNull { limiter.reserve(it.key, it.intervalMs) } ?: 0L
                Mono.delay(Duration.ofMillis(delayMs))
            })
            .then(Mono.defer { attempt(request, next, attemptNo + 1) })
    }

    companion object {
        private const val MAX_RETRY = 2
        private const val BACKOFF_MS = 300L

        internal fun isRetryableStatus(status: Int): Boolean = status == 429 || status >= 500
    }
}
