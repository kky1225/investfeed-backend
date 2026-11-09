package com.example.investfeed.domain.dividend.client

import com.example.investfeed.common.exception.ExternalApiFailureException
import com.example.investfeed.common.exception.InvestFeedException
import com.example.investfeed.common.util.apiFailure
import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.domain.dividend.client.dto.EtfDividendApiItem
import com.example.investfeed.domain.dividend.client.dto.EtfDividendApiResponse
import com.example.investfeed.domain.dividend.exception.EtfDividendApiException
import com.example.investfeed.domain.dividend.exception.EtfDividendResponseException
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import kotlin.coroutines.cancellation.CancellationException

@Component
class EtfDividendClient(
    @Qualifier("etfDividendWebClient")
    private val etfDividendWebClient: WebClient,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}

    /** 본문은 JSON 인데 Content-Type 이 text/html 로 와서 문자열로 받아 직접 변환한다 */
    suspend fun getDividendInfo(stkCd: String): List<EtfDividendApiItem> {
        try {
            val raw = etfDividendWebClient.get()
                .uri { it.path("/backend/get_all_dividends.php").queryParam("stock_code", stkCd).build() }
                .header(HttpHeaders.REFERER, "https://search-etf.com/$stkCd")
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("ETF 분배금"); throw EtfDividendApiException() })
                .awaitBodyOrNull<String>()
                .orEmpty()

            val response = objectMapper.readValue(raw, EtfDividendApiResponse::class.java)

            if (response.status != "success") {
                log.error { "ETF 분배금 API 응답 오류: status=${response.status}, stkCd=$stkCd" }
                throw EtfDividendResponseException()
            }

            return response.dividends ?: emptyList()
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "getDividendInfo Error: stkCd=$stkCd" }
            throw ExternalApiFailureException(e)
        }
    }
}
