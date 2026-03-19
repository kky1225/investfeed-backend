package com.example.investfeed.upbit.market.client

import com.example.investfeed.upbit.market.dto.res.UpbitMarketRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class MarketClient(
    @Qualifier("upbitWebClient")
    private val upbitWebClient: WebClient,
) {
    private val log = KotlinLogging.logger {}

    private var cachedMarkets: List<UpbitMarketRes> = emptyList()
    private var cacheTimestamp: Long = 0
    private val cacheDuration = 60 * 60 * 1000L // 1시간 캐시

    fun getMarketAll(): List<UpbitMarketRes> {
        val now = System.currentTimeMillis()
        if (cachedMarkets.isNotEmpty() && (now - cacheTimestamp) < cacheDuration) {
            return cachedMarkets
        }

        try {
            val res = upbitWebClient.get()
                .uri("/v1/market/all?is_details=true")
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<UpbitMarketRes>>() {})
                .block()

            log.info { "upbit getMarketAll: ${res?.size} markets" }

            cachedMarkets = res ?: emptyList()
            cacheTimestamp = now
            return cachedMarkets
        } catch (e: Exception) {
            log.error { "upbit getMarketAll Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    /**
     * KRW 마켓만 필터링하여 반환
     */
    fun getKrwMarkets(): List<UpbitMarketRes> {
        return getMarketAll().filter { it.market.startsWith("KRW-") }
    }
}
