package com.example.investfeed.domain.dividend.client

import com.example.investfeed.common.exception.ExternalApiFailureException
import com.example.investfeed.common.exception.InvestFeedException
import com.example.investfeed.common.util.apiFailure
import com.example.investfeed.domain.dividend.client.dto.DividendApiItem
import com.example.investfeed.domain.dividend.client.dto.DividendApiResponse
import com.example.investfeed.domain.dividend.exception.StockDividendApiException
import com.example.investfeed.domain.dividend.exception.StockDividendResponseException
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class StockDividendClient(
    @param:Value("\${data-go-kr.default-url}")
    private val defaultUrl: String,
    @param:Value("\${data-go-kr.service-key}")
    private val serviceKey: String,
    @Qualifier("stockDividendWebClient")
    private val stockDividendWebClient: WebClient,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}

    private val DIVIDEND_PATH = "/1160100/GetStocDiviInfoService_V2/getDiviInfo_V2"

    fun getDividendInfo(pageNo: Int, numOfRows: Int = 5000): Pair<List<DividendApiItem>, Int> {
        try {
            val encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8)
            val uri = URI("$defaultUrl$DIVIDEND_PATH?serviceKey=$encodedKey&pageNo=$pageNo&numOfRows=$numOfRows&resultType=json")

            // URL 에 서비스 키가 있어 logHttpError(URL 포함) 대신 상태·본문만 남긴다
            val raw = stockDividendWebClient.get()
                .uri(uri)
                .retrieve()
                .onStatus({ it.isError }, { res ->
                    res.bodyToMono(String::class.java).defaultIfEmpty("(no body)").map { body ->
                        log.error { "주식 배당 API HTTP 오류: status=${res.statusCode()}, pageNo=$pageNo, body=${body.take(500)}" }
                        StockDividendApiException()
                    }
                })
                .bodyToMono(String::class.java)
                .block()
                .orEmpty()

            val response = objectMapper.readValue(raw, DividendApiResponse::class.java)
            val body = response.response?.body ?: run {
                log.error { "주식 배당 API 응답 본문 없음: pageNo=$pageNo, raw=${raw.take(500)}" }
                throw StockDividendResponseException()
            }

            val items = body.items?.item ?: emptyList()
            val totalCount = body.totalCount ?: 0

            return Pair(items, totalCount)
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e, secretInUrl = true) { "getDividendInfo Error: pageNo=$pageNo" }
            throw ExternalApiFailureException()
        }
    }
}
