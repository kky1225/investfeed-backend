package com.example.investfeed.pressrelease.config

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.domain.monitoring.service.ApiCallCounterService
import com.example.investfeed.global.config.WebClientHttpClientFactory
import org.springframework.context.annotation.Bean
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class PressReleaseConfig(
    private val apiCallCounterService: ApiCallCounterService,
) {
    @Bean
    fun pressReleaseWebClient(): WebClient {
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(WebClientHttpClientFactory.createDefaultHttpClient()))
            .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; investfeed)")
            .codecs { config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) }
            .filter(ExchangeFilterFunction.ofRequestProcessor { req ->
                apiCallCounterService.increment(ApiProvider.PRESS_RELEASE)
                Mono.just(req)
            })
            .build()
    }
}
