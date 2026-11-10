package com.example.investfeed.telegram.config

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.global.config.ExternalWebClientFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Component
class TelegramConfig(
    @param:Value("\${telegram.api-url:https://api.telegram.org}")
    private val apiUrl: String,
    private val externalWebClientFactory: ExternalWebClientFactory,
) {

    companion object {
        // getUpdates 를 timeout=0(즉시 응답)으로 호출하므로 오래 기다릴 이유가 없다
        private val RESPONSE_TIMEOUT: Duration = Duration.ofSeconds(10)
    }

    @Bean
    fun telegramWebClient(): WebClient = externalWebClientFactory.create(
        provider = ApiProvider.TELEGRAM,
        baseUrl = apiUrl,
        headers = mapOf("Content-Type" to "application/json", "Accept" to "application/json"),
        responseTimeout = RESPONSE_TIMEOUT,
    )
}
