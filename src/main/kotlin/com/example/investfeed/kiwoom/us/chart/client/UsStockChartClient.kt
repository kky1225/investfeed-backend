package com.example.investfeed.kiwoom.us.chart.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.UsStockChartException
import com.example.investfeed.kiwoom.us.chart.dto.req.KiwoomUsStockChartReq
import com.example.investfeed.kiwoom.us.chart.dto.res.KiwoomUsStockChartRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class UsStockChartClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}
    private final val CHART_URL = "/api/us/chart"

    @KiwoomToken
    fun usStockMinuteChart(req: KiwoomUsStockChartReq): KiwoomUsStockChartRes = usStockChart("usa06011", req)

    @KiwoomToken
    fun usStockDayChart(req: KiwoomUsStockChartReq): KiwoomUsStockChartRes = usStockChart("usa06012", req)

    @KiwoomToken
    fun usStockWeekChart(req: KiwoomUsStockChartReq): KiwoomUsStockChartRes = usStockChart("usa06013", req)

    @KiwoomToken
    fun usStockMonthChart(req: KiwoomUsStockChartReq): KiwoomUsStockChartRes = usStockChart("usa06014", req)

    @KiwoomToken
    fun usStockYearChart(req: KiwoomUsStockChartReq): KiwoomUsStockChartRes = usStockChart("usa06015", req)

    private fun usStockChart(apiId: String, req: KiwoomUsStockChartReq): KiwoomUsStockChartRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + CHART_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", apiId)
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomUsStockChartRes::class.java)
                .block()

            if (res?.return_code != 0) {
                throw UsStockChartException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: UsStockChartException) {
            throw e
        } catch (e: Exception) {
            log.warn { "usStockChart Error : apiId=$apiId" }

            throw RuntimeException(e.message)
        }
    }
}
