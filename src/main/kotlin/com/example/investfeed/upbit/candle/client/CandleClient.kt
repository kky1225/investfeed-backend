package com.example.investfeed.upbit.candle.client

import com.example.investfeed.upbit.candle.dto.res.UpbitCandleDayRes
import com.example.investfeed.upbit.candle.dto.res.UpbitCandleMinuteRes
import com.example.investfeed.upbit.candle.dto.res.UpbitCandleWeekMonthRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class CandleClient(
    @Qualifier("upbitWebClient")
    private val upbitWebClient: WebClient,
) {
    private val log = KotlinLogging.logger {}

    fun getCandlesMinutes(unit: Int, market: String, count: Int = 200, to: String? = null): List<UpbitCandleMinuteRes> {
        try {
            val uri = buildString {
                append("/v1/candles/minutes/$unit?market=$market&count=$count")
                if (to != null) append("&to=$to")
            }

            val res = upbitWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<UpbitCandleMinuteRes>>() {})
                .block()

            log.info { "upbit getCandlesMinutes: unit=$unit, market=$market, to=$to" }

            return res ?: emptyList()
        } catch (e: Exception) {
            log.error { "upbit getCandlesMinutes Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    fun getCandlesDays(market: String, count: Int = 200, to: String? = null): List<UpbitCandleDayRes> {
        try {
            val uri = buildString {
                append("/v1/candles/days?market=$market&count=$count")
                if (to != null) append("&to=$to")
            }
            val res = upbitWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<UpbitCandleDayRes>>() {})
                .block()

            log.info { "upbit getCandlesDays: market=$market" }

            return res ?: emptyList()
        } catch (e: Exception) {
            log.error { "upbit getCandlesDays Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    fun getCandlesWeeks(market: String, count: Int = 200, to: String? = null): List<UpbitCandleWeekMonthRes> {
        try {
            val uri = buildString {
                append("/v1/candles/weeks?market=$market&count=$count")
                if (to != null) append("&to=$to")
            }
            val res = upbitWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<UpbitCandleWeekMonthRes>>() {})
                .block()

            log.info { "upbit getCandlesWeeks: market=$market" }

            return res ?: emptyList()
        } catch (e: Exception) {
            log.error { "upbit getCandlesWeeks Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    fun getCandlesYears(market: String, count: Int = 200, to: String? = null): List<UpbitCandleWeekMonthRes> {
        try {
            val uri = buildString {
                append("/v1/candles/years?market=$market&count=$count")
                if (to != null) append("&to=$to")
            }
            val res = upbitWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<UpbitCandleWeekMonthRes>>() {})
                .block()

            log.info { "upbit getCandlesYears: market=$market" }

            return res ?: emptyList()
        } catch (e: Exception) {
            log.error { "upbit getCandlesYears Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    fun getCandlesMonths(market: String, count: Int = 200, to: String? = null): List<UpbitCandleWeekMonthRes> {
        try {
            val uri = buildString {
                append("/v1/candles/months?market=$market&count=$count")
                if (to != null) append("&to=$to")
            }
            val res = upbitWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<UpbitCandleWeekMonthRes>>() {})
                .block()

            log.info { "upbit getCandlesMonths: market=$market" }

            return res ?: emptyList()
        } catch (e: Exception) {
            log.error { "upbit getCandlesMonths Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }
}
