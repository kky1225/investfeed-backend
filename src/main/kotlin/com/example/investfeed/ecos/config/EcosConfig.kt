package com.example.investfeed.ecos.config

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.global.config.ExternalWebClientFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class EcosConfig(
    @param:Value("\${ecos.default-url}")
    private val defaultUrl: String,
    @param:Value("\${ecos.api-key}")
    private val apiKey: String,
    private val externalWebClientFactory: ExternalWebClientFactory,
) {

    @Bean
    fun ecosWebClient(): WebClient = externalWebClientFactory.create(
        provider = ApiProvider.ECOS,
        baseUrl = defaultUrl,
        headers = mapOf("Content-Type" to "application/json", "Accept" to "application/json"),
        secrets = listOf(apiKey),   // 경로에 API 키 (EcosClient 와 같은 속성)
    )
}
