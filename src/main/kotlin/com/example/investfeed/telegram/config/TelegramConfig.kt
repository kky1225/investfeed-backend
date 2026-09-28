package com.example.investfeed.telegram.config

import com.example.investfeed.global.config.WebClientHttpClientFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class TelegramConfig(
    @param:Value("\${telegram.api-url:https://api.telegram.org}")
    private val apiUrl: String,
) {

    @Bean
    fun telegramWebClient(): WebClient {
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(WebClientHttpClientFactory.createDefaultHttpClient()))
            .baseUrl(apiUrl)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "application/json")
            .build()
    }
}
