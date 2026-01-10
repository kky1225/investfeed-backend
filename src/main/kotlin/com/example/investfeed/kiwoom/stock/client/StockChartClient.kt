package com.example.investfeed.kiwoom.stock.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.exception.*
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockChartDayReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockChartMinuteReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockChartMonthReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockChartWeekReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockChartYearReq
import com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockChartDayRes
import com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockChartMinuteRes
import com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockChartMonthRes
import com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockChartWeekRes
import com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockChartYearRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import kotlin.jvm.java

@Service
class StockChartClient(
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String

    @KiwoomToken
    fun chartMinuteList(
        req: KiwoomStockChartMinuteReq
    ): KiwoomStockChartMinuteRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10080")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockChartMinuteRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw ChartMinuteListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: ChartMinuteListException) {
            throw e
        }catch (e: Exception) {
            log.error { "chartMinuteList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun chartDayList(
        req: KiwoomStockChartDayReq
    ): KiwoomStockChartDayRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10081")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockChartDayRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw ChartDayListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: ChartDayListException) {
            throw e
        }catch (e: Exception) {
            log.error { "chartDayList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun chartWeekList(
        req: KiwoomStockChartWeekReq
    ): KiwoomStockChartWeekRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10082")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockChartWeekRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw ChartWeekListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: ChartWeekListException) {
            throw e
        }catch (e: Exception) {
            log.error { "chartWeekList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun chartMonthList(
        req: KiwoomStockChartMonthReq
    ): KiwoomStockChartMonthRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10083")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockChartMonthRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw ChartMonthListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: ChartMonthListException) {
            throw e
        }catch (e: Exception) {
            log.error { "chartMonthList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun chartYearList(
        req: KiwoomStockChartYearReq
    ): KiwoomStockChartYearRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10094")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockChartYearRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw ChartYearListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: ChartYearListException) {
            throw e
        }catch (e: Exception) {
            log.error { "chartYearList Error" }

            throw RuntimeException(e.message)
        }
    }
}