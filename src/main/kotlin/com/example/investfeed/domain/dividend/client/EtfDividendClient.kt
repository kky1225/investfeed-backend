package com.example.investfeed.domain.dividend.client

import com.example.investfeed.domain.dividend.client.dto.EtfDividendApiItem
import com.example.investfeed.domain.dividend.client.dto.EtfDividendApiResponse
import com.example.investfeed.domain.dividend.exception.EtfDividendApiException
import com.example.investfeed.domain.dividend.exception.EtfDividendResponseException
import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.domain.monitoring.service.ApiCallCounterService
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URI

@Component
class EtfDividendClient(
    private val objectMapper: ObjectMapper,
    private val apiCallCounterService: ApiCallCounterService,
) {
    private val log = KotlinLogging.logger {}

    private val BASE_URL = "https://search-etf.com/backend/get_all_dividends.php"

    fun getDividendInfo(stkCd: String): List<EtfDividendApiItem> {
        try {
            apiCallCounterService.increment(ApiProvider.SEARCH_ETF)

            val connection = URI("$BASE_URL?stock_code=$stkCd").toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.setRequestProperty("Referer", "https://search-etf.com/$stkCd")

            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                connection.disconnect()
                throw EtfDividendApiException()
            }

            val raw = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val response = objectMapper.readValue(raw, EtfDividendApiResponse::class.java)

            if (response.status != "success") {
                throw EtfDividendResponseException()
            }

            return response.dividends ?: emptyList()
        } catch (e: EtfDividendApiException) {
            throw e
        } catch (e: EtfDividendResponseException) {
            throw e
        } catch (e: Exception) {
            log.error { "getDividendInfo Error: ${e.message}" }

            throw RuntimeException(e.message)
        }
    }
}
