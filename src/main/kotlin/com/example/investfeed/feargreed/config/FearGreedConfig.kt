package com.example.investfeed.feargreed.config

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.global.config.ExternalWebClientFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class FearGreedConfig(
    @param:Value("\${fear-greed.default-url}")
    private val defaultUrl: String,
    private val externalWebClientFactory: ExternalWebClientFactory,
) {

    @Bean
    fun fearGreedWebClient(): WebClient = externalWebClientFactory.create(
        provider = ApiProvider.FEAR_GREED,
        baseUrl = defaultUrl,
        headers = mapOf("Content-Type" to "application/json", "Accept" to "application/json"),
    )
}
