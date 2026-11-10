package com.example.investfeed.naver.news.config

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.global.config.ExternalWebClientFactory
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class NaverNewsConfig(
    private val externalWebClientFactory: ExternalWebClientFactory,
) {
    @Bean
    fun naverNewsWebClient(): WebClient = externalWebClientFactory.create(
        provider = ApiProvider.NAVER_NEWS,
        baseUrl = "https://openapi.naver.com",
    )
}
