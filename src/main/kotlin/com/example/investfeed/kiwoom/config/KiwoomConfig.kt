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
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class KiwoomConfig(
    private val apiCallCounterService: ApiCallCounterService,
    private val rateLimitExchangeFilter: RateLimitExchangeFilter,
    private val kiwoomRateLimitRetryFilter: KiwoomRateLimitRetryFilter,
    private val redisTemplate: StringRedisTemplate,
) {

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
            .build()
    }
}
