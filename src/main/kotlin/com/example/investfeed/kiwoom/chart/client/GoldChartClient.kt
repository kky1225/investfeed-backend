package com.example.investfeed.kiwoom.chart.client

import com.example.investfeed.common.exception.ExternalApiFailureException
import com.example.investfeed.common.util.apiFailure
import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.chart.dto.gold.req.KiwoomGoldChartDayReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.KiwoomGoldChartMinuteReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.KiwoomGoldChartMonthReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.KiwoomGoldChartWeekReq
import com.example.investfeed.kiwoom.chart.dto.gold.res.KiwoomGoldChartDayRes
import com.example.investfeed.kiwoom.chart.dto.gold.res.KiwoomGoldChartMinuteRes
import com.example.investfeed.kiwoom.chart.dto.gold.res.KiwoomGoldChartMonthRes
import com.example.investfeed.kiwoom.chart.dto.gold.res.KiwoomGoldChartWeekRes
import com.example.investfeed.kiwoom.KiwoomRes
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.chart.dto.stock.res.KiwoomStockChartPage
import com.example.investfeed.kiwoom.chart.dto.stock.res.KiwoomStockChartRow
import com.example.investfeed.kiwoom.chart.enum.CommodityChartType
import com.example.investfeed.kiwoom.exception.GoldChartPageException
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.web.reactive.function.client.toEntity
import com.example.investfeed.kiwoom.exception.GoldChartDayListException
import com.example.investfeed.kiwoom.exception.GoldChartMinuteListException
import com.example.investfeed.kiwoom.exception.GoldChartMonthListException
import com.example.investfeed.kiwoom.exception.GoldChartWeekListException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import kotlin.coroutines.cancellation.CancellationException
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Service
class GoldChartClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient
) {
    private val log = KotlinLogging.logger {}

    private val CHART_URL = "/api/dostk/chart"

    @KiwoomToken
    suspend fun goldChartMinuteList (
        req: KiwoomGoldChartMinuteReq
    ): KiwoomGoldChartMinuteRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + CHART_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50092")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomGoldChartMinuteRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=goldChartMinuteList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw GoldChartMinuteListException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldChartMinuteListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "goldChartMinuteList Error" }

            throw ExternalApiFailureException(e)
        }
    }

    @KiwoomToken
    suspend fun goldChartDayList (
        req: KiwoomGoldChartDayReq
    ): KiwoomGoldChartDayRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + CHART_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50081")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomGoldChartDayRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=goldChartDayList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw GoldChartDayListException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldChartDayListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "goldChartDayList Error" }

            throw ExternalApiFailureException(e)
        }
    }

    @KiwoomToken
    suspend fun goldChartWeekList (
        req: KiwoomGoldChartWeekReq
    ): KiwoomGoldChartWeekRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + CHART_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50082")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomGoldChartWeekRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=goldChartWeekList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw GoldChartWeekListException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldChartWeekListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "goldChartWeekList Error" }

            throw ExternalApiFailureException(e)
        }
    }

    @KiwoomToken
    suspend fun goldChartMonthList (
        req: KiwoomGoldChartMonthReq
    ): KiwoomGoldChartMonthRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + CHART_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50083")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomGoldChartMonthRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=goldChartMonthList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw GoldChartMonthListException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldChartMonthListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "goldChartMonthList Error" }

            throw ExternalApiFailureException(e)
        }
    }

    /**
     * 금현물 차트 한 페이지를 받는다. nextKey 를 주면 그 이전 구간을 이어 받는다(연속 조회, cont-yn·next-key).
     * 일·주·월봉만 받는다. 분봉은 당일만 주는 TR(ka50092)을 쓰고 있어 이어 받을 과거가 없다(빈 페이지를 돌려준다).
     */
    @KiwoomToken
    suspend fun chartPage(chartType: CommodityChartType, stkCd: String, baseDt: String, nextKey: String?): KiwoomStockChartPage {
        try {
            return when (chartType) {
                CommodityChartType.DAY -> fetchPage<KiwoomGoldChartDayRes>(
                    "ka50081", KiwoomGoldChartDayReq(stk_cd = stkCd, base_dt = baseDt, upd_stkpc_tp = "1"), nextKey,
                ) { res -> res.gds_day_chart_qry?.map { KiwoomStockChartRow(it.dt, it.cur_prc, it.open_pric, it.high_pric, it.low_pric, it.acc_trde_qty, it.acc_trde_prica) } }

                CommodityChartType.WEEK -> fetchPage<KiwoomGoldChartWeekRes>(
                    "ka50082", KiwoomGoldChartWeekReq(stk_cd = stkCd, base_dt = baseDt, upd_stkpc_tp = "1"), nextKey,
                ) { res -> res.gds_week_chart_qry?.map { KiwoomStockChartRow(it.dt, it.cur_prc, it.open_pric, it.high_pric, it.low_pric, it.acc_trde_qty, it.acc_trde_prica) } }

                CommodityChartType.MONTH -> fetchPage<KiwoomGoldChartMonthRes>(
                    "ka50083", KiwoomGoldChartMonthReq(stk_cd = stkCd, base_dt = baseDt, upd_stkpc_tp = "1"), nextKey,
                ) { res -> res.gds_month_chart_qry?.map { KiwoomStockChartRow(it.dt, it.cur_prc, it.open_pric, it.high_pric, it.low_pric, it.acc_trde_qty, it.acc_trde_prica) } }

                else -> KiwoomStockChartPage(rows = emptyList(), nextKey = null)
            }
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldChartPageException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "goldChartPage Error : chartType=$chartType" }

            throw ExternalApiFailureException(e)
        }
    }

    /** 응답 헤더의 cont-yn 이 Y 일 때만 next-key 를 다음 구간의 키로 돌려준다. */
    private suspend inline fun <reified T : KiwoomRes> fetchPage(
        apiId: String,
        req: Any,
        nextKey: String?,
        toRows: (T) -> List<KiwoomStockChartRow>?,
    ): KiwoomStockChartPage {
        val accessToken = authClient.getCurrentAccessToken()
        val continued = !nextKey.isNullOrBlank()

        val entity = kiwoomWebClient.post()
            .uri(DEFAULT_URL + CHART_URL)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .header("api-id", apiId)
            .header("cont-yn", if (continued) "Y" else "N")
            .header("next-key", nextKey.orEmpty())
            .bodyValue(req)
            .retrieve()
            .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
            .toEntity<T>()
            .awaitSingleOrNull()

        val body = entity?.body
        if (body?.return_code != 0) {
            log.error { "키움 API 응답 오류: api=goldChartPage, apiId=$apiId, return_code=${body?.return_code}, return_msg=${body?.return_msg}, req=$req, continued=$continued" }
            throw GoldChartPageException()
        }

        val hasMore = entity.headers.getFirst("cont-yn") == "Y"
        return KiwoomStockChartPage(
            rows = toRows(body).orEmpty(),
            nextKey = entity.headers.getFirst("next-key")?.takeIf { hasMore && it.isNotBlank() },
        )
    }
}