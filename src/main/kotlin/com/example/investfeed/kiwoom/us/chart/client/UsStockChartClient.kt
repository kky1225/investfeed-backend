package com.example.investfeed.kiwoom.us.chart.client

import com.example.investfeed.common.exception.ExternalApiFailureException
import com.example.investfeed.common.util.apiFailure
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

/** 미국 차트의 주기와 그 TR */
enum class UsChartPeriod(val apiId: String) {
    MINUTE("usa06011"), DAY("usa06012"), WEEK("usa06013"), MONTH("usa06014"), YEAR("usa06015"),
}

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

    /** stopAt(yyyyMMdd): 받은 일봉이 이 날짜까지 내려오면 더 이어 받지 않는다 (비서 날짜 지정 조회) */
    @KiwoomToken
    fun usStockDayChart(req: KiwoomUsStockChartReq, stopAt: String? = null): KiwoomUsStockChartRes = usStockChart("usa06012", req, stopAt)

    @KiwoomToken
    fun usStockLatestTick(req: KiwoomUsStockChartReq): KiwoomUsStockChartItem? =
        usStockChart("usa06010", req.copy(tic_scope = "1"), maxPages = 1).result_list?.firstOrNull()

    @KiwoomToken
    fun usStockWeekChart(req: KiwoomUsStockChartReq): KiwoomUsStockChartRes = usStockChart("usa06013", req)

    @KiwoomToken
    fun usStockMonthChart(req: KiwoomUsStockChartReq): KiwoomUsStockChartRes = usStockChart("usa06014", req)

    @KiwoomToken
    fun usStockYearChart(req: KiwoomUsStockChartReq): KiwoomUsStockChartRes = usStockChart("usa06015", req)

    /**
     * 차트의 과거 구간 한 묶음(최대 MAX_PAGES 번 이어 받은 것). nextKey 가 없으면 가장 최근 구간이고,
     * 앞선 응답의 키를 주면 그 이전 구간이다. 돌려주는 키가 null 이면 더 받을 과거가 없다.
     */
    @KiwoomToken
    fun usStockChartPage(period: UsChartPeriod, req: KiwoomUsStockChartReq, nextKey: String?): Pair<KiwoomUsStockChartRes, String?> =
        usStockChartFrom(period.apiId, req, stopAt = null, maxPages = MAX_PAGES, startKey = nextKey)

    /**
     * 미국 차트는 1회 100건이라 cont-yn/next-key 로 최대 MAX_PAGES 까지 이어 받는다(국내 1회 600건과 길이 맞춤).
     * 페이지는 최신→과거 순으로 오므로 이어 붙이기만 하면 된다.
     */
    private fun usStockChart(apiId: String, req: KiwoomUsStockChartReq, stopAt: String? = null, maxPages: Int = MAX_PAGES): KiwoomUsStockChartRes =
        usStockChartFrom(apiId, req, stopAt, maxPages, startKey = null).first

    /**
     * startKey 가 있으면 그 키의 다음 구간부터 받는다. 돌려주는 두 번째 값은 그 뒤를 이어 받을 키이고, 더 없으면 null 이다.
     */
    private fun usStockChartFrom(
        apiId: String, req: KiwoomUsStockChartReq, stopAt: String?, maxPages: Int, startKey: String?,
    ): Pair<KiwoomUsStockChartRes, String?> {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val list = mutableListOf<KiwoomUsStockChartItem>()
            var contYn = if (startKey.isNullOrBlank()) "N" else "Y"
            var nextKey = startKey.orEmpty()
            var returnMsg = ""

            for (page in 1..maxPages) {
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
                if (stopAt != null && list.lastOrNull()?.dt?.let { it <= stopAt } == true) break
            }

            return KiwoomUsStockChartRes(
                return_code = 0,
                return_msg = returnMsg,
                result_list = list,
            ) to nextKey.takeIf { contYn == "Y" && it.isNotBlank() }
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: UsStockChartException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "usStockChart Error : apiId=$apiId" }

            throw ExternalApiFailureException(e)
        }
    }
}
