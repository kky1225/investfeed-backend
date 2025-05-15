package com.example.investfeed.kiwoom.config

import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class WebClientConfig {

    @Bean
    fun webClient(): WebClient {
        return WebClient.builder()
            .defaultHeader("Content-Type", "application/json")
            .codecs { config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) } // 5mb
            .build()
    }
}