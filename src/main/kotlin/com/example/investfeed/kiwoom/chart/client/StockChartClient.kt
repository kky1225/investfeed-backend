package com.example.investfeed.kiwoom.chart.client

import com.example.investfeed.common.exception.ExternalApiFailureException
import com.example.investfeed.common.util.apiFailure
import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.domain.monitoring.service.ApiCallCounterService
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.chart.dto.stock.req.*
import com.example.investfeed.kiwoom.chart.dto.stock.res.*
import com.example.investfeed.kiwoom.KiwoomRes
import com.example.investfeed.kiwoom.chart.enum.StockChartType
import com.example.investfeed.kiwoom.exception.*
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mu.KotlinLogging
import org.springframework.web.reactive.function.client.toEntity
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import com.example.investfeed.kiwoom.support.withQuotaRetry
import kotlin.coroutines.cancellation.CancellationException
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Service
class StockChartClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
    private val apiCallCounterService: ApiCallCounterService,
) {
    private val log = KotlinLogging.logger {}
    private val recordQuotaRetry = { apiCallCounterService.recordRetry(ApiProvider.KIWOOM, failureRecorded = true) }

    /**
     * 차트 한 페이지를 받는다. nextKey 를 주면 그 이전 구간을 이어 받는다(연속 조회, cont-yn·next-key).
     *
     * baseDt 는 항상 오늘로 둘 것. 과거 날짜를 기준일자로 주면 그 뒤에 생긴 권리(액면분할 등)가
     * 반영되지 않은 가격이 온다(명세 ka10081). 과거는 기준일자를 옮기지 말고 nextKey 로만 받는다.
     */
    @KiwoomToken
    suspend fun chartPage(chartType: StockChartType, stkCd: String, baseDt: String, nextKey: String?): KiwoomStockChartPage {
        try {
            return when (chartType) {
                StockChartType.DAY -> fetchPage<KiwoomStockChartDayRes>(
                    "ka10081", KiwoomStockChartDayReq(stk_cd = stkCd, base_dt = baseDt, upd_stkpc_tp = "1"), nextKey,
                ) { res -> res.stk_dt_pole_chart_qry?.map { KiwoomStockChartRow(it.dt, it.cur_prc, it.open_pric, it.high_pric, it.low_pric, it.trde_qty, it.trde_prica) } }

                StockChartType.WEEK -> fetchPage<KiwoomStockChartWeekRes>(
                    "ka10082", KiwoomStockChartWeekReq(stk_cd = stkCd, base_dt = baseDt, upd_stkpc_tp = "1"), nextKey,
                ) { res -> res.stk_stk_pole_chart_qry?.map { KiwoomStockChartRow(it.dt, it.cur_prc, it.open_pric, it.high_pric, it.low_pric, it.trde_qty, it.trde_prica) } }

                StockChartType.MONTH -> fetchPage<KiwoomStockChartMonthRes>(
                    "ka10083", KiwoomStockChartMonthReq(stk_cd = stkCd, base_dt = baseDt, upd_stkpc_tp = "1"), nextKey,
                ) { res -> res.stk_mth_pole_chart_qry?.map { KiwoomStockChartRow(it.dt, it.cur_prc, it.open_pric, it.high_pric, it.low_pric, it.trde_qty, it.trde_prica) } }

                StockChartType.YEAR -> fetchPage<KiwoomStockChartYearRes>(
                    "ka10094", KiwoomStockChartYearReq(stk_cd = stkCd, base_dt = baseDt, upd_stkpc_tp = "1"), nextKey,
                ) { res -> res.stk_yr_pole_chart_qry?.map { KiwoomStockChartRow(it.dt, it.cur_prc, it.open_pric, it.high_pric, it.low_pric, it.trde_qty, it.trde_prica) } }

                // 분봉: 기준일자 없이 최근부터. 체결시간이 캔들 시각이고 거래대금은 없다
                else -> fetchPage<KiwoomStockChartMinuteRes>(
                    "ka10080", KiwoomStockChartMinuteReq(stk_cd = stkCd, tic_scope = chartType.value ?: "1", upd_stkpc_tp = "1"), nextKey,
                ) { res -> res.stk_min_pole_chart_qry?.map { KiwoomStockChartRow(it.cntr_tm, it.cur_prc, it.open_pric, it.high_pric, it.low_pric, it.trde_qty, null) } }
            }
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: StockChartPageException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "chartPage Error : chartType=$chartType" }

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
            .uri("$DEFAULT_URL/api/dostk/chart")
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
            log.error { "키움 API 응답 오류: api=chartPage, apiId=$apiId, return_code=${body?.return_code}, return_msg=${body?.return_msg}, req=$req, continued=$continued" }
            throw StockChartPageException()
        }

        val hasMore = entity.headers.getFirst("cont-yn") == "Y"
        return KiwoomStockChartPage(
            rows = toRows(body).orEmpty(),
            nextKey = entity.headers.getFirst("next-key")?.takeIf { hasMore && it.isNotBlank() },
        )
    }

    @KiwoomToken
    suspend fun chartMinuteList(
        req: KiwoomStockChartMinuteReq
    ): KiwoomStockChartMinuteRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10080")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomStockChartMinuteRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=chartMinuteList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw StockChartMinuteListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: StockChartMinuteListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            log.apiFailure(e) { "chartMinuteList Error" }

            throw ExternalApiFailureException(e)
        }
    }

    @KiwoomToken
    suspend fun chartDayList(
        req: KiwoomStockChartDayReq
    ): KiwoomStockChartDayRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = withQuotaRetry("ka10081", { it.return_code }, onRetry = recordQuotaRetry) { kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10081")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomStockChartDayRes>() }

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=chartDayList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw StockChartDayListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: StockChartDayListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            log.apiFailure(e) { "chartDayList Error" }

            throw ExternalApiFailureException(e)
        }
    }

    @KiwoomToken
    suspend fun chartWeekList(
        req: KiwoomStockChartWeekReq
    ): KiwoomStockChartWeekRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10082")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomStockChartWeekRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=chartWeekList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw StockChartWeekListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: StockChartWeekListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            log.apiFailure(e) { "chartWeekList Error" }

            throw ExternalApiFailureException(e)
        }
    }

    @KiwoomToken
    suspend fun chartMonthList(
        req: KiwoomStockChartMonthReq
    ): KiwoomStockChartMonthRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10083")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomStockChartMonthRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=chartMonthList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw StockChartMonthListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: StockChartMonthListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            log.apiFailure(e) { "chartMonthList Error" }

            throw ExternalApiFailureException(e)
        }
    }

    @KiwoomToken
    suspend fun chartYearList(
        req: KiwoomStockChartYearReq
    ): KiwoomStockChartYearRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10094")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomStockChartYearRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=chartYearList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw StockChartYearListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: StockChartYearListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            log.apiFailure(e) { "chartYearList Error" }

            throw ExternalApiFailureException(e)
        }
    }

    @KiwoomToken
    suspend fun stockChartInvestor(
        req: KiwoomStockChartInvestorReq
    ): KiwoomStockChartInvestorRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10064")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomStockChartInvestorRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=stockChartInvestor, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw StockChartInvestorException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: StockChartInvestorException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            log.apiFailure(e) { "stockChartInvestor Error" }

            throw ExternalApiFailureException(e)
        }
    }
}