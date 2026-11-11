package com.example.investfeed.upbit.candle.client

import com.example.investfeed.common.exception.ExternalApiFailureException
import com.example.investfeed.common.exception.InvestFeedException
import com.example.investfeed.common.util.apiFailure
import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.upbit.candle.dto.res.UpbitCandleDayRes
import com.example.investfeed.upbit.candle.dto.res.UpbitCandleMinuteRes
import com.example.investfeed.upbit.candle.dto.res.UpbitCandleWeekMonthRes
import com.example.investfeed.upbit.exception.UpbitApiException
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
                .onStatus({ it.isError }, { res -> res.logHttpError("업비트"); throw UpbitApiException() })
                .bodyToMono(object : ParameterizedTypeReference<List<UpbitCandleMinuteRes>>() {})
                .block()

            return res ?: emptyList()
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "upbit getCandlesMinutes Error: unit=$unit, market=$market" }
            throw ExternalApiFailureException(e)
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
                .onStatus({ it.isError }, { res -> res.logHttpError("업비트"); throw UpbitApiException() })
                .bodyToMono(object : ParameterizedTypeReference<List<UpbitCandleDayRes>>() {})
                .block()

            return res ?: emptyList()
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "upbit getCandlesDays Error: market=$market" }
            throw ExternalApiFailureException(e)
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
                .onStatus({ it.isError }, { res -> res.logHttpError("업비트"); throw UpbitApiException() })
                .bodyToMono(object : ParameterizedTypeReference<List<UpbitCandleWeekMonthRes>>() {})
                .block()

            return res ?: emptyList()
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "upbit getCandlesWeeks Error: market=$market" }
            throw ExternalApiFailureException(e)
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
                .onStatus({ it.isError }, { res -> res.logHttpError("업비트"); throw UpbitApiException() })
                .bodyToMono(object : ParameterizedTypeReference<List<UpbitCandleWeekMonthRes>>() {})
                .block()

            return res ?: emptyList()
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "upbit getCandlesYears Error: market=$market" }
            throw ExternalApiFailureException(e)
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
                .onStatus({ it.isError }, { res -> res.logHttpError("업비트"); throw UpbitApiException() })
                .bodyToMono(object : ParameterizedTypeReference<List<UpbitCandleWeekMonthRes>>() {})
                .block()

            return res ?: emptyList()
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "upbit getCandlesMonths Error: market=$market" }
            throw ExternalApiFailureException(e)
        }
    }
}
