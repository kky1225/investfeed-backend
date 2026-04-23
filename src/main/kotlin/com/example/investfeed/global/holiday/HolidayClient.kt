package com.example.investfeed.global.holiday

import com.example.investfeed.global.holiday.exception.HolidayApiException
import com.example.investfeed.global.holiday.exception.HolidayInfoException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory

@Component
class HolidayClient(
    @param:Value("\${data-go-kr.default-url}")
    private val DEFAULT_URL: String,
    @param:Value("\${data-go-kr.service-key}")
    private val serviceKey: String,
) {
    private val log = KotlinLogging.logger {}

    private val HOLIDAY_PATH = "/B090041/openapi/service/SpcdeInfoService/getRestDeInfo"

    data class HolidayInfo(val date: String, val name: String)

    fun getHolidays(year: Int, month: Int): List<String> {
        return getHolidayInfos(year, month).map { it.date }
    }

    fun getHolidayInfos(year: Int, month: Int): List<HolidayInfo> {
        val solMonth = String.format("%02d", month)

        try {
            val encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8)
            val urlStr = "$DEFAULT_URL$HOLIDAY_PATH?serviceKey=$encodedKey&solYear=$year&solMonth=$solMonth&numOfRows=30"
            val connection = URI(urlStr).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode !in 200..299) {
                connection.disconnect()
                throw HolidayApiException()
            }

            val xml = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val resultCode = parseResultCode(xml)
            if (resultCode != "00") {
                throw HolidayInfoException()
            }

            return parseHolidayInfos(xml)
        } catch (e: HolidayApiException) {
            throw e
        } catch (e: HolidayInfoException) {
            throw e
        } catch (e: Exception) {
            log.warn { "getHolidayInfos Error: ${e.message}" }

            throw RuntimeException(e.message)
        }
    }

    private fun parseResultCode(xml: String): String? {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(ByteArrayInputStream(xml.toByteArray()))
        val resultCodeNode = document.getElementsByTagName("resultCode").item(0)
        return resultCodeNode?.textContent
    }

    private fun parseHolidayInfos(xml: String): List<HolidayInfo> {
        val holidays = mutableListOf<HolidayInfo>()

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(ByteArrayInputStream(xml.toByteArray()))

        val items = document.getElementsByTagName("item")
        for (i in 0 until items.length) {
            val item = items.item(i) as Element
            val isHoliday = item.getElementsByTagName("isHoliday").item(0)?.textContent
            val locdate = item.getElementsByTagName("locdate").item(0)?.textContent
            val dateName = item.getElementsByTagName("dateName").item(0)?.textContent

            if (isHoliday == "Y" && locdate != null) {
                holidays.add(HolidayInfo(date = locdate, name = dateName ?: "공휴일"))
            }
        }

        return holidays
    }
}
