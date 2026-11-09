package com.example.investfeed.domain.dividend.config

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.domain.monitoring.service.ApiCallCounterService
import com.example.investfeed.domain.monitoring.service.ApiCallMetricsFilter
import com.example.investfeed.global.config.WebClientHttpClientFactory
import org.springframework.context.annotation.Bean
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class DividendConfig(
    private val apiCallCounterService: ApiCallCounterService,
) {
    /**
     * 공공데이터포털 주식배당. 한 번에 5,000건을 받아 응답이 수 MB 라 크기·대기 시간을 넉넉히 둔다.
     * 서비스 키는 직접 인코딩해 완성한 URI 로 넘긴다 (uri(String) 은 키를 한 번 더 인코딩한다)
     */
    @Bean
    fun stockDividendWebClient(): WebClient {
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(WebClientHttpClientFactory.createDefaultHttpClient(Duration.ofSeconds(60))))
            .codecs { config -> config.defaultCodecs().maxInMemorySize(20 * 1024 * 1024) }
            .filter(ExchangeFilterFunction.ofRequestProcessor { req ->
                apiCallCounterService.increment(ApiProvider.PUBLIC_DATA_DIVIDEND)
                Mono.just(req)
            })
            .filter(ApiCallMetricsFilter(ApiProvider.PUBLIC_DATA_DIVIDEND, apiCallCounterService))
            .build()
    }

    /** search-etf.com (비공식). 브라우저 요청이 아니면 막힐 수 있어 User-Agent 를 붙인다 */
    @Bean
    fun etfDividendWebClient(): WebClient {
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(WebClientHttpClientFactory.createDefaultHttpClient()))
            .baseUrl("https://search-etf.com")
            .defaultHeader("User-Agent", "Mozilla/5.0")
            .codecs { config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) }
            .filter(ExchangeFilterFunction.ofRequestProcessor { req ->
                apiCallCounterService.increment(ApiProvider.SEARCH_ETF)
                Mono.just(req)
            })
            .filter(ApiCallMetricsFilter(ApiProvider.SEARCH_ETF, apiCallCounterService))
            .build()
    }
}
