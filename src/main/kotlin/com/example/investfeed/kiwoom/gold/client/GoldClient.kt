package com.example.investfeed.kiwoom.gold.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.chart.dto.gold.req.GoldChartDayListReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.GoldChartMinuteListReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.GoldChartMonthListReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.GoldChartWeekListReq
import com.example.investfeed.kiwoom.chart.dto.gold.res.GoldChartDayListRes
import com.example.investfeed.kiwoom.chart.dto.gold.res.GoldChartMinuteListRes
import com.example.investfeed.kiwoom.chart.dto.gold.res.GoldChartMonthListRes
import com.example.investfeed.kiwoom.chart.dto.gold.res.GoldChartWeekListRes
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
import com.example.investfeed.kiwoom.exception.GoldChartDayListException
import com.example.investfeed.kiwoom.exception.GoldChartMinuteListException
import com.example.investfeed.kiwoom.exception.GoldChartMonthListException
import com.example.investfeed.kiwoom.exception.GoldChartWeekListException
import com.example.investfeed.kiwoom.exception.GoldInvestorException
import com.example.investfeed.kiwoom.exception.GoldPriceNowException
import com.example.investfeed.kiwoom.exception.GoldPriceNowMinuteException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.gold.dto.rest.req.GoldPriceNowMinuteReq
import com.example.investfeed.kiwoom.gold.dto.rest.req.GoldPriceNowReq
import com.example.investfeed.kiwoom.gold.dto.rest.res.GoldInvestorRes
import com.example.investfeed.kiwoom.gold.dto.rest.res.GoldPriceNowMinuteRes
import com.example.investfeed.kiwoom.gold.dto.rest.res.GoldPriceNowRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class GoldClient(
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String

    @KiwoomToken
    fun goldPriceNow(
        req: GoldPriceNowReq
    ): GoldPriceNowRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/mrkcond")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50100")
                .bodyValue(req)
                .retrieve()
                .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                .bodyToMono(GoldPriceNowRes::class.java)
                .block()

            if (res?.return_code != 0) {
                throw GoldPriceNowException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldPriceNowException) {
            throw e;
        } catch (e: Exception) {
            log.error { "goldPriceNow Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun goldPriceNowMinute(
        req: GoldPriceNowMinuteReq
    ): GoldPriceNowMinuteRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/mrkcond")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50101")
                .bodyValue(req)
                .retrieve()
                .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                .bodyToMono(GoldPriceNowMinuteRes::class.java)
                .block()

            if (res?.return_code != 0) {
                throw GoldPriceNowMinuteException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldPriceNowMinuteException) {
            throw e;
        } catch (e: Exception) {
            log.error { "goldPriceNowMinute Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun goldInvestor(): GoldInvestorRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/frgnistt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka52301")
                .bodyValue("{}")
                .retrieve()
                .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                .bodyToMono(GoldInvestorRes::class.java)
                .block()

            if (res?.return_code != 0) {
                throw GoldInvestorException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldInvestorException) {
            throw e;
        } catch (e: Exception) {
            log.error { "goldInvestor Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun goldChartMinuteList (
        req: GoldChartMinuteListReq
    ): GoldChartMinuteListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50092")
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

    @KiwoomToken
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

    @KiwoomToken
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

    @KiwoomToken
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