package com.example.investfeed.fred.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class FredConfig(
    @param:Value("\${fred.default-url}")
    private val defaultUrl: String,
) {

    @Bean
    fun fredWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(defaultUrl)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "application/json")
            .codecs { config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) }
            .build()
    }
}
