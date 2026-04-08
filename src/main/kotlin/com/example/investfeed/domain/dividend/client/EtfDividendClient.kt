package com.example.investfeed.domain.dividend.client

import com.example.investfeed.domain.dividend.client.dto.EtfDividendApiItem
import com.example.investfeed.domain.dividend.client.dto.EtfDividendApiResponse
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URI

@Component
class EtfDividendClient(
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}

    private val BASE_URL = "https://search-etf.com/backend/get_all_dividends.php"

    fun getDividendInfo(stkCd: String): List<EtfDividendApiItem> {
        try {
            val connection = URI("$BASE_URL?stock_code=$stkCd").toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.setRequestProperty("Referer", "https://search-etf.com/$stkCd")

            val raw = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val response = objectMapper.readValue(raw, EtfDividendApiResponse::class.java)
            if (response.status != "success") return emptyList()

            return response.dividends ?: emptyList()
        } catch (e: Exception) {
            log.error(e) { "ETF 분배금 API 호출 실패: stkCd=$stkCd" }
            return emptyList()
        }
    }
}
