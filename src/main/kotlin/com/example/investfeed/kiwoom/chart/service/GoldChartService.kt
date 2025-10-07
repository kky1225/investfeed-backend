package com.example.investfeed.kiwoom.chart.service

import com.example.investfeed.kiwoom.chart.dto.gold.req.GoldChartDayListReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.GoldChartMinuteListReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.GoldChartMonthListReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.GoldChartWeekListReq
import com.example.investfeed.kiwoom.chart.dto.gold.res.GoldChartDayListRes
import com.example.investfeed.kiwoom.chart.dto.gold.res.GoldChartMinuteListRes
import com.example.investfeed.kiwoom.chart.dto.gold.res.GoldChartMonthListRes
import com.example.investfeed.kiwoom.chart.dto.gold.res.GoldChartWeekListRes
import com.example.investfeed.kiwoom.exception.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class GoldChartService (
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String

    fun goldChartMinuteList (
        req: GoldChartMinuteListReq
    ): GoldChartMinuteListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50080")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(GoldChartMinuteListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw GoldChartMinuteListException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldChartMinuteListException) {
            throw e
        } catch (e: Exception) {
            log.error { "goldChartMinuteList Error" }

            throw RuntimeException(e.message)
        }
    }

    fun goldChartDayList (
        req: GoldChartDayListReq
    ): GoldChartDayListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50081")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(GoldChartDayListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw GoldChartDayListException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldChartDayListException) {
            throw e
        } catch (e: Exception) {
            log.error { "goldChartDayList Error" }

            throw RuntimeException(e.message)
        }
    }

    fun goldChartWeekList (
        req: GoldChartWeekListReq
    ): GoldChartWeekListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50082")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(GoldChartWeekListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw GoldChartWeekListException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldChartWeekListException) {
            throw e
        } catch (e: Exception) {
            log.error { "goldChartWeekList Error" }

            throw RuntimeException(e.message)
        }
    }

    fun goldChartMonthList (
        req: GoldChartMonthListReq
    ): GoldChartMonthListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50083")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(GoldChartMonthListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw GoldChartMonthListException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldChartMonthListException) {
            throw e
        } catch (e: Exception) {
            log.error { "goldChartMonthList Error" }

            throw RuntimeException(e.message)
        }
    }
}