package com.example.investfeed.global.holiday

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

@Component
class HolidayConfig(
    private val apiCallCounterService: ApiCallCounterService,
) {
    /** 공공데이터포털 서비스 키는 직접 인코딩해 완성한 URI 로 넘긴다 (uri(String) 은 키를 한 번 더 인코딩한다) */
    @Bean
    fun holidayWebClient(): WebClient {
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(WebClientHttpClientFactory.createDefaultHttpClient()))
            .codecs { config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) }
            .filter(ExchangeFilterFunction.ofRequestProcessor { req ->
                apiCallCounterService.increment(ApiProvider.PUBLIC_DATA_HOLIDAY)
                Mono.just(req)
            })
            .filter(ApiCallMetricsFilter(ApiProvider.PUBLIC_DATA_HOLIDAY, apiCallCounterService))
            .build()
    }
}
