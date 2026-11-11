package com.example.investfeed.upbit.market.client

import com.example.investfeed.common.exception.ExternalApiFailureException
import com.example.investfeed.common.exception.InvestFeedException
import com.example.investfeed.common.util.apiFailure
import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.upbit.exception.UpbitApiException
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
                .onStatus({ it.isError }, { res -> res.logHttpError("업비트"); throw UpbitApiException() })
                .bodyToMono(object : ParameterizedTypeReference<List<UpbitMarketRes>>() {})
                .block()

            cachedMarkets = res ?: emptyList()
            cacheTimestamp = now
            return cachedMarkets
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "upbit getMarketAll Error" }
            throw ExternalApiFailureException(e)
        }
    }

    /**
     * KRW 마켓만 필터링하여 반환
     */
    fun getKrwMarkets(): List<UpbitMarketRes> {
        return getMarketAll().filter { it.market.startsWith("KRW-") }
    }
}
