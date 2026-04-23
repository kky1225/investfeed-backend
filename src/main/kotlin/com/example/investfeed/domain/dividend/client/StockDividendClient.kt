package com.example.investfeed.domain.dividend.client

import com.example.investfeed.domain.dividend.client.dto.DividendApiItem
import com.example.investfeed.domain.dividend.client.dto.DividendApiResponse
import com.example.investfeed.domain.dividend.exception.StockDividendApiException
import com.example.investfeed.domain.dividend.exception.StockDividendResponseException
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class StockDividendClient(
    @param:Value("\${data-go-kr.default-url}")
    private val defaultUrl: String,
    @param:Value("\${data-go-kr.service-key}")
    private val serviceKey: String,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}

    private val DIVIDEND_PATH = "/1160100/service/GetStocDiviInfoService/getDiviInfo"

    fun getDividendInfo(pageNo: Int, numOfRows: Int = 5000): Pair<List<DividendApiItem>, Int> {
        try {
            val encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8)
            val urlStr = "$defaultUrl$DIVIDEND_PATH?serviceKey=$encodedKey&pageNo=$pageNo&numOfRows=$numOfRows&resultType=json"
            val connection = URI(urlStr).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                connection.disconnect()
                throw StockDividendApiException()
            }

            val raw = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val response = objectMapper.readValue(raw, DividendApiResponse::class.java)
            val body = response.response?.body ?: throw StockDividendResponseException()

            val items = body.items?.item ?: emptyList()
            val totalCount = body.totalCount ?: 0

            return Pair(items, totalCount)
        } catch (e: StockDividendApiException) {
            throw e
        } catch (e: StockDividendResponseException) {
            throw e
        } catch (e: Exception) {
            log.error { "getDividendInfo Error: ${e.message}" }

            throw RuntimeException(e.message)
        }
    }
}
