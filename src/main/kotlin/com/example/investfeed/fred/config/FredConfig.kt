package com.example.investfeed.fred.config

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.global.config.ExternalWebClientFactory
import com.example.investfeed.global.ratelimit.FredRetryFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class FredConfig(
    @param:Value("\${fred.default-url}")
    private val defaultUrl: String,
    private val externalWebClientFactory: ExternalWebClientFactory,
    private val fredRetryFilter: FredRetryFilter,
) {

    @Bean
    fun fredWebClient(): WebClient = externalWebClientFactory.create(
        provider = ApiProvider.FRED,
        baseUrl = defaultUrl,
        headers = mapOf("Content-Type" to "application/json", "Accept" to "application/json"),
        innerFilters = listOf(fredRetryFilter),
    )
}
