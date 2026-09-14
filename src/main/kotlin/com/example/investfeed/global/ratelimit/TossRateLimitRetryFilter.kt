package com.example.investfeed.global.ratelimit

import mu.KotlinLogging
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.time.Duration
import kotlin.random.Random

@Component
class TossRateLimitRetryFilter(
    private val limiter: MinIntervalRateLimiter,
    private val policy: RateLimitPolicy,
    private val props: RateLimitProperties,
) : ExchangeFilterFunction {

    private val log = KotlinLogging.logger {}

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> = attempt(request, next, 0)

    private fun attempt(request: ClientRequest, next: ExchangeFunction, attemptNo: Int): Mono<ClientResponse> {
        return next.exchange(request).flatMap { response ->
            val headers = response.headers().asHttpHeaders()
            val limit = headers.getFirst(HEADER_LIMIT)
            val remaining = headers.getFirst(HEADER_REMAINING)?.toLongOrNull()
            val reset = headers.getFirst(HEADER_RESET)?.toDoubleOrNull()
            val path = request.url().path
            log.debug { "토스 rate-limit path=$path status=${response.statusCode().value()} limit=$limit remaining=$remaining reset=$reset" }

            val isTooMany = response.statusCode().value() == 429
            if (isTooMany && attemptNo < props.toss429MaxRetry && request.method() == HttpMethod.GET) {
                val waitMs = headers.getFirst(HEADER_RETRY_AFTER)?.toDoubleOrNull()?.let { (it * 1000).toLong() }
                    ?: backoffMs(attemptNo)
                log.warn { "토스 429 수신 — ${waitMs}ms 후 재시도(${attemptNo + 1}/${props.toss429MaxRetry}): path=$path" }
                response.releaseBody()
                    .then(Mono.delay(Duration.ofMillis(waitMs)))
                    .then(Mono.defer { attempt(request, next, attemptNo + 1) })
            } else {
                if (!isTooMany && remaining == 0L && reset != null && reset > 0) {
                    val holdMs = (reset * 1000).toLong()
                    policy.resolve(request).forEach { limiter.holdFor(it.key, holdMs) }
                    log.debug { "토스 버킷 소진 — ${holdMs}ms 선제 대기: path=$path" }
                }
                Mono.just(response)
            }
        }
    }

    private fun backoffMs(attemptNo: Int): Long = (BASE_BACKOFF_MS shl attemptNo) + Random.nextLong(0, JITTER_MS)

    companion object {
        private const val HEADER_LIMIT = "X-RateLimit-Limit"
        private const val HEADER_REMAINING = "X-RateLimit-Remaining"
        private const val HEADER_RESET = "X-RateLimit-Reset"
        private const val HEADER_RETRY_AFTER = "Retry-After"
        private const val BASE_BACKOFF_MS = 1_000L
        private const val JITTER_MS = 300L
    }
}
