package com.example.investfeed.kiwoom.chart.client

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
import com.example.investfeed.kiwoom.auth.service.AuthClient
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
            log.warn { "sectChartMinuteList Error" }

            throw RuntimeException(e.message)
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
            log.warn { "sectChartDayList Error" }

            throw RuntimeException(e.message)
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
            log.warn { "sectChartWeekList Error" }

            throw RuntimeException(e.message)
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
            log.warn { "sectChartMonthList Error" }

            throw RuntimeException(e.message)
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
            log.warn { "sectChartYearList Error" }

            throw RuntimeException(e.message)
        }
    }
}