package com.example.investfeed.kiwoom.chart.client

import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.chart.dto.stock.req.*
import com.example.investfeed.kiwoom.chart.dto.stock.res.*
import com.example.investfeed.kiwoom.exception.*
import mu.KotlinLogging
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
    private val authClient: AuthClient
) {
    private val log = KotlinLogging.logger {}

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
            log.warn { "chartMinuteList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    suspend fun chartDayList(
        req: KiwoomStockChartDayReq
    ): KiwoomStockChartDayRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = withQuotaRetry("ka10081", { it.return_code }) { kiwoomWebClient.post()
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
            log.warn { "chartDayList Error" }

            throw RuntimeException(e.message)
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
            log.warn { "chartWeekList Error" }

            throw RuntimeException(e.message)
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
            log.warn { "chartMonthList Error" }

            throw RuntimeException(e.message)
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
            log.warn { "chartYearList Error" }

            throw RuntimeException(e.message)
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
            log.warn { "stockChartInvestor Error" }

            throw RuntimeException(e.message)
        }
    }
}