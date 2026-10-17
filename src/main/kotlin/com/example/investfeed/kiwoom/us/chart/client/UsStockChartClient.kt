package com.example.investfeed.kiwoom.us.chart.client

import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.UsStockChartException
import com.example.investfeed.kiwoom.us.chart.dto.req.KiwoomUsStockChartReq
import com.example.investfeed.kiwoom.us.chart.dto.res.KiwoomUsStockChartItem
import com.example.investfeed.kiwoom.us.chart.dto.res.KiwoomUsStockChartRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity

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
    private final val MAX_PAGES = 6

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

    /**
     * 미국 차트는 1회 100건이라 cont-yn/next-key 로 최대 MAX_PAGES 까지 이어 받는다(국내 1회 600건과 길이 맞춤).
     * 페이지는 최신→과거 순으로 오므로 이어 붙이기만 하면 된다.
     */
    private fun usStockChart(apiId: String, req: KiwoomUsStockChartReq): KiwoomUsStockChartRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val list = mutableListOf<KiwoomUsStockChartItem>()
            var contYn = "N"
            var nextKey = ""
            var returnMsg = ""

            for (page in 1..MAX_PAGES) {
                val entity = kiwoomWebClient.post()
                    .uri(DEFAULT_URL + CHART_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", apiId)
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                    .toEntity<KiwoomUsStockChartRes>()
                    .block()

                val body = entity?.body
                if (body?.return_code != 0) {
                    log.error { "키움 API 응답 오류: api=usStockChart, return_code=${body?.return_code}, return_msg=${body?.return_msg}, apiId=$apiId, req=$req, page=$page" }
                    throw UsStockChartException()
                }

                returnMsg = body.return_msg
                body.result_list?.let { items -> list.addAll(items) }

                contYn = entity.headers.getFirst("cont-yn") ?: "N"
                nextKey = entity.headers.getFirst("next-key") ?: ""

                if (contYn != "Y" || nextKey.isBlank()) break
            }

            return KiwoomUsStockChartRes(
                return_code = 0,
                return_msg = returnMsg,
                result_list = list,
            )
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
