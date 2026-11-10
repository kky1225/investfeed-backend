package com.example.investfeed.global.holiday

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.global.config.ExternalWebClientFactory
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class HolidayConfig(
    private val externalWebClientFactory: ExternalWebClientFactory,
) {
    /** 공공데이터포털 서비스 키는 직접 인코딩해 완성한 URI 로 넘긴다 (uri(String) 은 키를 한 번 더 인코딩한다) */
    @Bean
    fun holidayWebClient(): WebClient = externalWebClientFactory.create(ApiProvider.PUBLIC_DATA_HOLIDAY)
}
