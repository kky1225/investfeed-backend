package com.example.investfeed.kiwoom.chart.client

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
import org.springframework.web.reactive.function.client.bodyToMono

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
    fun chartMinuteList(
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
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockChartMinuteRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockChartMinuteListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: StockChartMinuteListException) {
            throw e
        }catch (e: Exception) {
            log.warn { "chartMinuteList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun chartDayList(
        req: KiwoomStockChartDayReq
    ): KiwoomStockChartDayRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10081")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockChartDayRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockChartDayListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: StockChartDayListException) {
            throw e
        }catch (e: Exception) {
            log.warn { "chartDayList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun chartWeekList(
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
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockChartWeekRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockChartWeekListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: StockChartWeekListException) {
            throw e
        }catch (e: Exception) {
            log.warn { "chartWeekList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun chartMonthList(
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
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockChartMonthRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockChartMonthListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: StockChartMonthListException) {
            throw e
        }catch (e: Exception) {
            log.warn { "chartMonthList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun chartYearList(
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
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockChartYearRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockChartYearListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: StockChartYearListException) {
            throw e
        }catch (e: Exception) {
            log.warn { "chartYearList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockChartInvestor(
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
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomStockChartInvestorRes>()
                .block()

            if(res?.return_code != 0) {
                throw StockChartInvestorException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: StockChartInvestorException) {
            throw e
        }catch (e: Exception) {
            log.warn { "stockChartInvestor Error" }

            throw RuntimeException(e.message)
        }
    }
}