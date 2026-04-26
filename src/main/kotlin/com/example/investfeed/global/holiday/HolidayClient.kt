package com.example.investfeed.global.holiday

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.domain.monitoring.service.ApiCallCounterService
import com.example.investfeed.global.holiday.dto.HolidayApiResponse
import com.example.investfeed.global.holiday.exception.HolidayApiException
import com.example.investfeed.global.holiday.exception.HolidayInfoException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class HolidayClient(
    @param:Value("\${data-go-kr.default-url}")
    private val DEFAULT_URL: String,
    @param:Value("\${data-go-kr.service-key}")
    private val serviceKey: String,
    private val apiCallCounterService: ApiCallCounterService,
) {
    private val log = KotlinLogging.logger {}

    private val HOLIDAY_PATH = "/B090041/openapi/service/SpcdeInfoService/getRestDeInfo"

    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
        configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    data class HolidayInfo(val date: String, val name: String)

    fun getHolidays(year: Int, month: Int): List<String> {
        return getHolidayInfos(year, month).map { it.date }
    }

    fun getHolidayInfos(year: Int, month: Int): List<HolidayInfo> {
        val solMonth = String.format("%02d", month)

        try {
            apiCallCounterService.increment(ApiProvider.PUBLIC_DATA_HOLIDAY)

            val encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8)
            val urlStr = "$DEFAULT_URL$HOLIDAY_PATH?serviceKey=$encodedKey&solYear=$year&solMonth=$solMonth&numOfRows=30&_type=json"
            val connection = URI(urlStr).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode !in 200..299) {
                connection.disconnect()
                throw HolidayApiException()
            }

            val raw = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val response = objectMapper.readValue(raw, HolidayApiResponse::class.java)

            val resultCode = response.response?.header?.resultCode
            if (resultCode != "00") {
                throw HolidayInfoException()
            }

            val items = response.response.body?.items?.item ?: emptyList()
            return items
                .filter { it.isHoliday == "Y" && it.locdate != null }
                .map { HolidayInfo(date = it.locdate.toString(), name = it.dateName ?: "공휴일") }
        } catch (e: HolidayApiException) {
            throw e
        } catch (e: HolidayInfoException) {
            throw e
        } catch (e: Exception) {
            log.warn { "getHolidayInfos Error: ${e.message}" }

            throw RuntimeException(e.message)
        }
    }
}
