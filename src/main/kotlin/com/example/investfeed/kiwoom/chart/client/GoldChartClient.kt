package com.example.investfeed.kiwoom.chart.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.chart.dto.gold.req.KiwoomGoldChartDayReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.KiwoomGoldChartMinuteReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.KiwoomGoldChartMonthReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.KiwoomGoldChartWeekReq
import com.example.investfeed.kiwoom.chart.dto.gold.res.KiwoomGoldChartDayRes
import com.example.investfeed.kiwoom.chart.dto.gold.res.KiwoomGoldChartMinuteRes
import com.example.investfeed.kiwoom.chart.dto.gold.res.KiwoomGoldChartMonthRes
import com.example.investfeed.kiwoom.chart.dto.gold.res.KiwoomGoldChartWeekRes
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
import com.example.investfeed.kiwoom.exception.GoldChartDayListException
import com.example.investfeed.kiwoom.exception.GoldChartMinuteListException
import com.example.investfeed.kiwoom.exception.GoldChartMonthListException
import com.example.investfeed.kiwoom.exception.GoldChartWeekListException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class GoldChartClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val log = KotlinLogging.logger {}

    private val CHART_URL = "/api/dostk/chart"

    @KiwoomToken
    fun goldChartMinuteList (
        req: KiwoomGoldChartMinuteReq
    ): KiwoomGoldChartMinuteRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + CHART_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50092")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomGoldChartMinuteRes::class.java)
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

    @KiwoomToken
    fun goldChartDayList (
        req: KiwoomGoldChartDayReq
    ): KiwoomGoldChartDayRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + CHART_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50081")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomGoldChartDayRes::class.java)
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

    @KiwoomToken
    fun goldChartWeekList (
        req: KiwoomGoldChartWeekReq
    ): KiwoomGoldChartWeekRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + CHART_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50082")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomGoldChartWeekRes::class.java)
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

    @KiwoomToken
    fun goldChartMonthList (
        req: KiwoomGoldChartMonthReq
    ): KiwoomGoldChartMonthRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + CHART_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50083")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomGoldChartMonthRes::class.java)
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