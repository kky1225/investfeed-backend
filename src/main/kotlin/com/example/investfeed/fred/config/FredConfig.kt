package com.example.investfeed.fred.config

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.domain.monitoring.service.ApiCallCounterService
import io.netty.channel.ChannelOption
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

@Component
class FredConfig(
    @param:Value("\${fred.default-url}")
    private val defaultUrl: String,
    private val apiCallCounterService: ApiCallCounterService,
) {

    @Bean
    fun fredWebClient(): WebClient {
        val httpClient = HttpClient.create(ConnectionProvider.newConnection())
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
            .responseTimeout(Duration.ofSeconds(60))

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .baseUrl(defaultUrl)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "application/json")
            .codecs { config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) }
            .filter(ExchangeFilterFunction.ofRequestProcessor { req ->
                apiCallCounterService.increment(ApiProvider.FRED)
                Mono.just(req)
            })
            .build()
    }
}
