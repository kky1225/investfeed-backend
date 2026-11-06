package com.example.investfeed.kiwoom.config

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.domain.monitoring.service.ApiCallCounterService
import com.example.investfeed.domain.monitoring.service.ApiCallMetricsFilter
import com.example.investfeed.global.config.WebClientHttpClientFactory
import com.example.investfeed.global.ratelimit.KiwoomRateLimitRetryFilter
import com.example.investfeed.global.ratelimit.RateLimitExchangeFilter
import com.example.investfeed.global.token.RejectedTokenFilter
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClientRequest
import java.time.Duration

@Component
class KiwoomConfig(
    private val apiCallCounterService: ApiCallCounterService,
    private val rateLimitExchangeFilter: RateLimitExchangeFilter,
    private val kiwoomRateLimitRetryFilter: KiwoomRateLimitRetryFilter,
    private val redisTemplate: StringRedisTemplate,
) {

    companion object {
        private val ORDER_RESPONSE_TIMEOUT: Duration = Duration.ofSeconds(60)
        private val ORDER_API_ID_PREFIXES = listOf("kt10", "kt50")
    }

    @Bean
    fun kiwoomWebClient(): WebClient {
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(WebClientHttpClientFactory.createDefaultHttpClient()))
            .defaultHeader("Content-Type", "application/json")
            .codecs { config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) } // 5mb
            .filter(ExchangeFilterFunction.ofRequestProcessor { req ->
                apiCallCounterService.increment(ApiProvider.KIWOOM)
                Mono.just(req)
            })
            .filter(rateLimitExchangeFilter)
            .filter(ApiCallMetricsFilter(ApiProvider.KIWOOM, apiCallCounterService, ApiCallMetricsFilter.Companion::isKiwoomFailureBody))
            .filter(kiwoomRateLimitRetryFilter)
            .filter(RejectedTokenFilter.kiwoom(redisTemplate))
            .filter(orderResponseTimeoutFilter())
            .build()
    }

    /** 주문은 시간 초과가 나도 체결 여부를 알 수 없으므로 조회보다 길게 기다린다 */
    private fun orderResponseTimeoutFilter() = ExchangeFilterFunction { request, next ->
        val apiId = request.headers().getFirst("api-id")
        if (apiId != null && ORDER_API_ID_PREFIXES.any { apiId.startsWith(it) }) {
            next.exchange(
                ClientRequest.from(request)
                    .httpRequest { it.getNativeRequest<HttpClientRequest>().responseTimeout(ORDER_RESPONSE_TIMEOUT) }
                    .build()
            )
        } else {
            next.exchange(request)
        }
    }
}
