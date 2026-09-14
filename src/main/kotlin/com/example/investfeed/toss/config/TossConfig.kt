package com.example.investfeed.toss.config

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.domain.monitoring.service.ApiCallCounterService
import com.example.investfeed.global.config.WebClientHttpClientFactory
import com.example.investfeed.global.ratelimit.RateLimitExchangeFilter
import com.example.investfeed.global.ratelimit.TossRateLimitRetryFilter
import org.springframework.context.annotation.Bean
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class TossConfig(
    private val apiCallCounterService: ApiCallCounterService,
    private val rateLimitExchangeFilter: RateLimitExchangeFilter,
    private val tossRateLimitRetryFilter: TossRateLimitRetryFilter,
) {

    @Bean
    fun tossWebClient(): WebClient {
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(WebClientHttpClientFactory.createDefaultHttpClient()))
            .defaultHeader("Accept", "application/json")
            .codecs { config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) } // 5mb
            .filter(ExchangeFilterFunction.ofRequestProcessor { req ->
                apiCallCounterService.increment(ApiProvider.TOSS)
                Mono.just(req)
            })
            .filter(rateLimitExchangeFilter)
            .filter(tossRateLimitRetryFilter)
            .build()
    }
}
