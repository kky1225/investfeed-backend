package com.example.investfeed.pressrelease.config

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.global.config.ExternalWebClientFactory
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class PressReleaseConfig(
    private val externalWebClientFactory: ExternalWebClientFactory,
) {
    @Bean
    fun pressReleaseWebClient(): WebClient = externalWebClientFactory.create(
        provider = ApiProvider.PRESS_RELEASE,
        headers = mapOf("User-Agent" to "Mozilla/5.0 (compatible; investfeed)"),
    )
}
