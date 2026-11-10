package com.example.investfeed.toss.config

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.global.config.ExternalWebClientFactory
import com.example.investfeed.global.ratelimit.TossRateLimitRetryFilter
import com.example.investfeed.global.token.RejectedTokenFilter
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class TossConfig(
    private val externalWebClientFactory: ExternalWebClientFactory,
    private val tossRateLimitRetryFilter: TossRateLimitRetryFilter,
    private val redisTemplate: StringRedisTemplate,
) {

    @Bean
    fun tossWebClient(): WebClient = externalWebClientFactory.create(
        provider = ApiProvider.TOSS,
        headers = mapOf("Accept" to "application/json"),
        innerFilters = listOf(tossRateLimitRetryFilter, RejectedTokenFilter.toss(redisTemplate)),
    )
}
