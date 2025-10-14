package com.example.investfeed.kiwoom.gold.service

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
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
}