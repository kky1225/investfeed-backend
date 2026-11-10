package com.example.investfeed.global.holiday

import com.example.investfeed.common.exception.ExternalApiFailureException
import com.example.investfeed.common.exception.InvestFeedException
import com.example.investfeed.common.util.apiFailure
import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.global.holiday.dto.HolidayApiResponse
import com.example.investfeed.global.holiday.exception.HolidayApiException
import com.example.investfeed.global.holiday.exception.HolidayInfoException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class HolidayClient(
    @param:Value("\${data-go-kr.default-url}")
    private val DEFAULT_URL: String,
    @param:Value("\${data-go-kr.service-key}")
    private val serviceKey: String,
    @Qualifier("holidayWebClient")
    private val holidayWebClient: WebClient,
) {
    private val log = KotlinLogging.logger {}

    private val HOLIDAY_PATH = "/B090041/openapi/service/SpcdeInfoService/getRestDeInfo"

    private val NUM_OF_ROWS = 100

    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
        configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    data class HolidayInfo(val date: String, val name: String)

    fun getHolidayInfos(year: Int): List<HolidayInfo> {
        try {
            val encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8)
            val uri = URI("$DEFAULT_URL$HOLIDAY_PATH?serviceKey=$encodedKey&solYear=$year&numOfRows=$NUM_OF_ROWS&_type=json")

            val raw = holidayWebClient.get()
                .uri(uri)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("공휴일", secrets = listOf(encodedKey)); throw HolidayApiException() })
                .bodyToMono(String::class.java)
                .block()
                .orEmpty()

            val response = objectMapper.readValue(raw, HolidayApiResponse::class.java)

            val resultCode = response.response?.header?.resultCode
            if (resultCode != "00") {
                log.error { "공휴일 API 응답 오류: resultCode=$resultCode, resultMsg=${response.response?.header?.resultMsg}, year=$year" }
                throw HolidayInfoException()
            }

            val items = response.response.body?.items?.item ?: emptyList()

            val totalCount = response.response.body?.totalCount
            if (totalCount != null && totalCount > items.size) {
                log.error { "공휴일 API 응답 누락: ${year}년 totalCount=$totalCount, 수신=${items.size} (numOfRows=$NUM_OF_ROWS)" }
                throw HolidayInfoException()
            }

            return items
                .filter { it.isHoliday == "Y" && it.locdate != null }
                .map { HolidayInfo(date = it.locdate.toString(), name = it.dateName ?: "공휴일") }
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e, secretInUrl = true) { "getHolidayInfos Error: year=$year" }
            throw ExternalApiFailureException()
        }
    }
}
