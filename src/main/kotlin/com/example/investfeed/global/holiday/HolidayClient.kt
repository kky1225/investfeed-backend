package com.example.investfeed.global.holiday

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

@Component
class HolidayClient(
    @param:Value("\${data-go-kr.default-url}")
    private val DEFAULT_URL: String,
    @param:Value("\${data-go-kr.service-key}")
    private val serviceKey: String,
    private val webClient: WebClient,
) {
    private val log = KotlinLogging.logger {}

    private val HOLIDAY_PATH = "/B090041/openapi/service/SpcdeInfoService/getRestDeInfo"

    fun getHolidays(year: Int, month: Int): List<String> {
        val solMonth = String.format("%02d", month)

        try {
            val xml = webClient.get()
                .uri("$DEFAULT_URL$HOLIDAY_PATH?serviceKey=$serviceKey&solYear=$year&solMonth=$solMonth&numOfRows=30")
                .retrieve()
                .bodyToMono(String::class.java)
                .block() ?: return emptyList()

            return parseHolidayDates(xml)
        } catch (e: Exception) {
            log.error(e) { "공휴일 API 호출 실패: $year-$solMonth" }
            return emptyList()
        }
    }

    private fun parseHolidayDates(xml: String): List<String> {
        val dates = mutableListOf<String>()

        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(ByteArrayInputStream(xml.toByteArray()))

            val items = document.getElementsByTagName("item")
            for (i in 0 until items.length) {
                val item = items.item(i) as Element
                val isHoliday = item.getElementsByTagName("isHoliday").item(0)?.textContent
                val locdate = item.getElementsByTagName("locdate").item(0)?.textContent

                if (isHoliday == "Y" && locdate != null) {
                    dates.add(locdate)
                }
            }
        } catch (e: Exception) {
            log.error(e) { "공휴일 XML 파싱 실패" }
        }

        return dates
    }
}
