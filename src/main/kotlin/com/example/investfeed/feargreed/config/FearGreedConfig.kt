package com.example.investfeed.feargreed.config

import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class FearGreedConfig {

    @Bean("fearGreedWebClient")
    fun fearGreedWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl("https://api.alternative.me")
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "application/json")
            .codecs { config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) }
            .build()
    }
}
