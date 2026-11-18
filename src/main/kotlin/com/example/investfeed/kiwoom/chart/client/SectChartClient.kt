package com.example.investfeed.kiwoom.chart.client

import com.example.investfeed.common.exception.ExternalApiFailureException
import com.example.investfeed.common.util.apiFailure
import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartDayListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartMinuteListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartMonthListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartWeekListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartYearListReq
import com.example.investfeed.kiwoom.chart.dto.sect.res.KiwoomSectChartDayRes
import com.example.investfeed.kiwoom.chart.dto.sect.res.KiwoomSectChartMinuteRes
import com.example.investfeed.kiwoom.chart.dto.sect.res.KiwoomSectChartMonthRes
import com.example.investfeed.kiwoom.chart.dto.sect.res.KiwoomSectChartWeekRes
import com.example.investfeed.kiwoom.chart.dto.sect.res.KiwoomSectChartYearRes
import com.example.investfeed.kiwoom.KiwoomRes
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.chart.dto.stock.res.KiwoomStockChartPage
import com.example.investfeed.kiwoom.chart.dto.stock.res.KiwoomStockChartRow
import com.example.investfeed.kiwoom.chart.enum.IndexChartType
import com.example.investfeed.kiwoom.exception.SectChartPageException
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.web.reactive.function.client.toEntity
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.SectChartDayListException
import com.example.investfeed.kiwoom.exception.SectChartMinuteListException
import com.example.investfeed.kiwoom.exception.SectChartMonthListException
import com.example.investfeed.kiwoom.exception.SectChartWeekListException
import com.example.investfeed.kiwoom.exception.SectChartYearListException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import kotlin.coroutines.cancellation.CancellationException
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Service
class SectChartClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient
) {
    private val log = KotlinLogging.logger {}

    @KiwoomToken
    suspend fun sectChartMinuteList(
        req: SectChartMinuteListReq
    ): KiwoomSectChartMinuteRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20005")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomSectChartMinuteRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=sectChartMinuteList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw SectChartMinuteListException()
            }

            val today = res.inds_min_pole_qry?.get(0)?.cntr_tm?.substring(0, 8)

            res.inds_min_pole_qry = res.inds_min_pole_qry?.filter { it.cntr_tm?.startsWith(today ?: "") == true }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: SectChartMinuteListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            log.apiFailure(e) { "sectChartMinuteList Error" }

            throw ExternalApiFailureException(e)
        }
    }

    @KiwoomToken
    suspend fun sectChartDayList(
        req: SectChartDayListReq
    ): KiwoomSectChartDayRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20006")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomSectChartDayRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=sectChartDayList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw SectChartDayListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: SectChartDayListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            log.apiFailure(e) { "sectChartDayList Error" }

            throw ExternalApiFailureException(e)
        }
    }

    @KiwoomToken
    suspend fun sectChartWeekList(
        req: SectChartWeekListReq
    ): KiwoomSectChartWeekRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20007")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomSectChartWeekRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=sectChartWeekList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw SectChartWeekListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: SectChartWeekListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            log.apiFailure(e) { "sectChartWeekList Error" }

            throw ExternalApiFailureException(e)
        }
    }

    @KiwoomToken
    suspend fun sectChartMonthList(
        req: SectChartMonthListReq
    ): KiwoomSectChartMonthRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20008")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomSectChartMonthRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=sectChartMonthList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw SectChartMonthListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: SectChartMonthListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            log.apiFailure(e) { "sectChartMonthList Error" }

            throw ExternalApiFailureException(e)
        }
    }

    @KiwoomToken
    suspend fun sectChartYearList(
        req: SectChartYearListReq
    ): KiwoomSectChartYearRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20019")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomSectChartYearRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=sectChartYearList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw SectChartYearListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: SectChartYearListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            log.apiFailure(e) { "sectChartYearList Error" }

            throw ExternalApiFailureException(e)
        }
    }

    /**
     * 업종 차트 한 페이지를 받는다. nextKey 를 주면 그 이전 구간을 이어 받는다(연속 조회, cont-yn·next-key).
     * 분봉은 당일로 자르지 않고 받은 그대로 준다. 가격은 다른 업종 차트와 같이 100배로 온다.
     */
    @KiwoomToken
    suspend fun chartPage(chartType: IndexChartType, indsCd: String, baseDt: String, nextKey: String?): KiwoomStockChartPage {
        try {
            return when (chartType) {
                IndexChartType.DAY -> fetchPage<KiwoomSectChartDayRes>(
                    "ka20006", SectChartDayListReq(inds_cd = indsCd, base_dt = baseDt), nextKey,
                ) { res -> res.inds_dt_pole_qry?.map { KiwoomStockChartRow(it.dt, it.cur_prc, it.open_pric, it.high_pric, it.low_pric, it.trde_qty, it.trde_prica) } }

                IndexChartType.WEEK -> fetchPage<KiwoomSectChartWeekRes>(
                    "ka20007", SectChartWeekListReq(inds_cd = indsCd, base_dt = baseDt), nextKey,
                ) { res -> res.inds_stk_pole_qry?.map { KiwoomStockChartRow(it.dt, it.cur_prc, it.open_pric, it.high_pric, it.low_pric, it.trde_qty, it.trde_prica) } }

                IndexChartType.MONTH -> fetchPage<KiwoomSectChartMonthRes>(
                    "ka20008", SectChartMonthListReq(inds_cd = indsCd, base_dt = baseDt), nextKey,
                ) { res -> res.inds_mth_pole_qry?.map { KiwoomStockChartRow(it.dt, it.cur_prc, it.open_pric, it.high_pric, it.low_pric, it.trde_qty, it.trde_prica) } }

                IndexChartType.YEAR -> fetchPage<KiwoomSectChartYearRes>(
                    "ka20019", SectChartYearListReq(inds_cd = indsCd, base_dt = baseDt), nextKey,
                ) { res -> res.inds_yr_pole_qry?.map { KiwoomStockChartRow(it.dt, it.cur_prc, it.open_pric, it.high_pric, it.low_pric, it.trde_qty, it.trde_prica) } }

                else -> fetchPage<KiwoomSectChartMinuteRes>(
                    "ka20005", SectChartMinuteListReq(inds_cd = indsCd, tic_scope = chartType.value ?: "1"), nextKey,
                ) { res -> res.inds_min_pole_qry?.map { KiwoomStockChartRow(it.cntr_tm, it.cur_prc, it.open_pric, it.high_pric, it.low_pric, it.trde_qty, it.acc_trde_qty) } }
            }
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: SectChartPageException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "sectChartPage Error : chartType=$chartType" }

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
            log.error { "키움 API 응답 오류: api=sectChartPage, apiId=$apiId, return_code=${body?.return_code}, return_msg=${body?.return_msg}, req=$req, continued=$continued" }
            throw SectChartPageException()
        }

        val hasMore = entity.headers.getFirst("cont-yn") == "Y"
        return KiwoomStockChartPage(
            rows = toRows(body).orEmpty(),
            nextKey = entity.headers.getFirst("next-key")?.takeIf { hasMore && it.isNotBlank() },
        )
    }
}