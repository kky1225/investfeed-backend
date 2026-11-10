package com.example.investfeed.domain.dividend.config

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.global.config.ExternalWebClientFactory
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Component
class DividendConfig(
    private val externalWebClientFactory: ExternalWebClientFactory,
) {
    /**
     * 공공데이터포털 주식배당. 한 번에 5,000건을 받아 응답이 수 MB 라 크기·대기 시간을 넉넉히 둔다.
     * 서비스 키는 직접 인코딩해 완성한 URI 로 넘긴다 (uri(String) 은 키를 한 번 더 인코딩한다)
     */
    @Bean
    fun stockDividendWebClient(): WebClient = externalWebClientFactory.create(
        provider = ApiProvider.PUBLIC_DATA_DIVIDEND,
        responseTimeout = Duration.ofSeconds(60),
        maxInMemoryBytes = 20 * 1024 * 1024,
    )

    /** search-etf.com (비공식). 브라우저 요청이 아니면 막힐 수 있어 User-Agent 를 붙인다 */
    @Bean
    fun etfDividendWebClient(): WebClient = externalWebClientFactory.create(
        provider = ApiProvider.SEARCH_ETF,
        baseUrl = "https://search-etf.com",
        headers = mapOf("User-Agent" to "Mozilla/5.0"),
    )
}
