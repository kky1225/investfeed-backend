package com.example.investfeed.upbit.ticker.client

import com.example.investfeed.upbit.ticker.dto.res.UpbitTickerRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class TickerClient(
    @Qualifier("upbitWebClient")
    private val upbitWebClient: WebClient,
) {
    private val log = KotlinLogging.logger {}

    fun getTickers(markets: String): List<UpbitTickerRes> {
        try {
            val res = upbitWebClient.get()
                .uri("/v1/ticker?markets=$markets")
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<UpbitTickerRes>>() {})
                .block()

            log.info { "upbit getTickers: $markets" }

            return res ?: emptyList()
        } catch (e: Exception) {
            log.error { "upbit getTickers Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }
}
