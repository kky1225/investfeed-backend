package com.example.investfeed.kiwoom.chart.service

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.chart.dto.req.ChartDayListReq
import com.example.investfeed.kiwoom.chart.dto.req.ChartMinuteListReq
import com.example.investfeed.kiwoom.chart.dto.req.ChartMonthListReq
import com.example.investfeed.kiwoom.chart.dto.req.ChartTickListReq
import com.example.investfeed.kiwoom.chart.dto.req.ChartWeekListReq
import com.example.investfeed.kiwoom.chart.dto.req.ChartYearListReq
import com.example.investfeed.kiwoom.chart.dto.res.ChartDayListRes
import com.example.investfeed.kiwoom.chart.dto.res.ChartMinuteListRes
import com.example.investfeed.kiwoom.chart.dto.res.ChartMonthListRes
import com.example.investfeed.kiwoom.chart.dto.res.ChartTickListRes
import com.example.investfeed.kiwoom.chart.dto.res.ChartWeekListRes
import com.example.investfeed.kiwoom.chart.dto.res.ChartYearListRes
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
import com.example.investfeed.kiwoom.exception.ChartDayListException
import com.example.investfeed.kiwoom.exception.ChartMinuteListException
import com.example.investfeed.kiwoom.exception.ChartMonthListException
import com.example.investfeed.kiwoom.exception.ChartTickListException
import com.example.investfeed.kiwoom.exception.ChartWeekListException
import com.example.investfeed.kiwoom.exception.ChartYearListException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class ChartService(
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String

    @KiwoomToken
    fun chartTickList(
        req: ChartTickListReq
    ): ChartTickListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10079")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(ChartTickListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw ChartTickListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: ChartTickListException) {
            throw e
        }catch (e: Exception) {
            log.error { "chartTickList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun chartMinuteList(
        req: ChartMinuteListReq
    ): ChartMinuteListRes? {
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
                .bodyToMono(ChartMinuteListRes::class.java)
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
        req: ChartDayListReq
    ): ChartDayListRes? {
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
                .bodyToMono(ChartDayListRes::class.java)
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
        req: ChartWeekListReq
    ): ChartWeekListRes? {
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
                .bodyToMono(ChartWeekListRes::class.java)
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
        req: ChartMonthListReq
    ): ChartMonthListRes? {
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
                .bodyToMono(ChartMonthListRes::class.java)
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
        req: ChartYearListReq
    ): ChartYearListRes? {
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
                .bodyToMono(ChartYearListRes::class.java)
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