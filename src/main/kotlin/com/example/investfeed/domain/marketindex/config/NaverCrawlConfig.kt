package com.example.investfeed.domain.marketindex.config

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.global.config.ExternalWebClientFactory
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class NaverCrawlConfig(
    private val externalWebClientFactory: ExternalWebClientFactory,
) {
    /** 네이버 증권(비공식). 요청 주소가 여러 호스트라 baseUrl 없이 전체 주소로 부른다 */
    @Bean
    fun naverCrawlWebClient(): WebClient = externalWebClientFactory.create(
        provider = ApiProvider.NAVER_CRAWL,
        headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        ),
    )
}
