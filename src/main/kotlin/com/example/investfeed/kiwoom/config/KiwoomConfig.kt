package com.example.investfeed.kiwoom.config

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.domain.monitoring.service.ApiCallMetricsFilter
import com.example.investfeed.global.config.ExternalWebClientFactory
import com.example.investfeed.global.ratelimit.KiwoomRateLimitRetryFilter
import com.example.investfeed.global.token.RejectedTokenFilter
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClientRequest
import java.time.Duration

@Component
class KiwoomConfig(
    private val externalWebClientFactory: ExternalWebClientFactory,
    private val kiwoomRateLimitRetryFilter: KiwoomRateLimitRetryFilter,
    private val redisTemplate: StringRedisTemplate,
) {

    companion object {
        private val ORDER_RESPONSE_TIMEOUT: Duration = Duration.ofSeconds(60)
        private val ORDER_API_ID_PREFIXES = listOf("kt10", "kt50")
    }

    @Bean
    fun kiwoomWebClient(): WebClient = externalWebClientFactory.create(
        provider = ApiProvider.KIWOOM,
        headers = mapOf("Content-Type" to "application/json"),
        isFailureBody = ApiCallMetricsFilter.Companion::isKiwoomFailureBody,
        innerFilters = listOf(kiwoomRateLimitRetryFilter, RejectedTokenFilter.kiwoom(redisTemplate), orderResponseTimeoutFilter()),
    )

    /** 주문은 시간 초과가 나도 체결 여부를 알 수 없으므로 조회보다 길게 기다린다 */
    private fun orderResponseTimeoutFilter() = ExchangeFilterFunction { request, next ->
        val apiId = request.headers().getFirst("api-id")
        if (apiId != null && ORDER_API_ID_PREFIXES.any { apiId.startsWith(it) }) {
            next.exchange(
                ClientRequest.from(request)
                    .httpRequest { it.getNativeRequest<HttpClientRequest>().responseTimeout(ORDER_RESPONSE_TIMEOUT) }
                    .build()
            )
        } else {
            next.exchange(request)
        }
    }
}
