package com.example.investfeed.global.config

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.domain.monitoring.service.ApiCallCounterService
import com.example.investfeed.domain.monitoring.service.ApiCallMetricsFilter
import com.example.investfeed.global.ratelimit.RateLimitExchangeFilter
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * 외부 API 용 WebClient 는 모두 여기서 만든다 (API 마다 WebClient 는 따로, 만드는 방식은 하나).
 *
 * 필터 순서 (바깥 → 안):
 * 1. 호출 건수 집계
 * 2. 호출 간격 제한 — 대상 호스트·간격은 RateLimitPolicy 가 정한다 (규칙 없는 호스트는 그대로 통과)
 * 3. 실패·응답 시간 집계(ApiCallMetricsFilter) — 재시도로 성공한 호출이 실패로 잡히지 않도록 재시도 필터 바깥
 * 4. innerFilters — API 별 재시도·토큰 거부·타임아웃 등
 */
@Component
class ExternalWebClientFactory(
    private val apiCallCounterService: ApiCallCounterService,
    private val rateLimitExchangeFilter: RateLimitExchangeFilter,
) {
    companion object {
        private const val DEFAULT_MAX_IN_MEMORY_BYTES = 5 * 1024 * 1024
    }

    /**
     * [secrets]: 요청 경로에 들어가는 API 키·토큰의 실제 값 (ECOS 키, 텔레그램 봇 토큰).
     * 호출 상세 debug 로그에서 경로는 남기고 이 값 자리만 가린다
     */
    fun create(
        provider: ApiProvider,
        baseUrl: String? = null,
        headers: Map<String, String> = emptyMap(),
        responseTimeout: Duration? = null,
        maxInMemoryBytes: Int = DEFAULT_MAX_IN_MEMORY_BYTES,
        isFailureBody: ((ByteArray) -> Boolean)? = null,
        secrets: List<String> = emptyList(),
        innerFilters: List<ExchangeFilterFunction> = emptyList(),
    ): WebClient {
        val httpClient = responseTimeout
            ?.let { WebClientHttpClientFactory.createDefaultHttpClient(it) }
            ?: WebClientHttpClientFactory.createDefaultHttpClient()

        val builder = WebClient.builder().clientConnector(ReactorClientHttpConnector(httpClient))
        baseUrl?.let { builder.baseUrl(it) }
        headers.forEach { (name, value) -> builder.defaultHeader(name, value) }

        return builder
            .codecs { config -> config.defaultCodecs().maxInMemorySize(maxInMemoryBytes) }
            .filter(ExchangeFilterFunction.ofRequestProcessor { req ->
                apiCallCounterService.increment(provider)
                Mono.just(req)
            })
            .filter(rateLimitExchangeFilter)
            .filter(ApiCallMetricsFilter(provider, apiCallCounterService, isFailureBody, secrets))
            .filters { it.addAll(innerFilters) }
            .build()
    }
}
