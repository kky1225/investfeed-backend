package com.example.investfeed.global.ratelimit

import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class RateLimitExchangeFilter(
    private val limiter: MinIntervalRateLimiter,
    private val policy: RateLimitPolicy,
    private val props: RateLimitProperties,
) : ExchangeFilterFunction {

    private val log = KotlinLogging.logger {}

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> = Mono.defer {
        val rules = policy.resolve(request)
        val delayMs = rules.maxOfOrNull { limiter.reserve(it.key, it.intervalMs) } ?: 0L

        if (delayMs <= 0) {
            next.exchange(request)
        } else {
            if (props.logDelaysOverMs > 0 && delayMs > props.logDelaysOverMs) {
                log.debug { "rate-limit 대기 ${delayMs}ms: ${rules.joinToString { it.key }}" }
            }
            Mono.delay(Duration.ofMillis(delayMs)).then(Mono.defer { next.exchange(request) })
        }
    }
}
