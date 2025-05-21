package com.example.investfeed.kiwoom.etf.service

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.etf.dto.req.EtfPriceListReq
import com.example.investfeed.kiwoom.etf.dto.req.EtfInfoReq
import com.example.investfeed.kiwoom.etf.dto.req.EtfTradeDailyListReq
import com.example.investfeed.kiwoom.etf.dto.res.EtfPriceListRes
import com.example.investfeed.kiwoom.etf.dto.res.EtfInfoRes
import com.example.investfeed.kiwoom.etf.dto.res.EtfTradeDailyListRes
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
import com.example.investfeed.kiwoom.exception.EtfInfoException
import com.example.investfeed.kiwoom.exception.EtfTradeDailyListException
import com.example.investfeed.kiwoom.exception.EtfPriceListException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class EtfService(
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String

    @KiwoomToken
    fun etfPriceList(
        req: EtfPriceListReq
    ): EtfPriceListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/etf")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka40004")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(EtfPriceListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw EtfPriceListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: EtfPriceListException) {
            throw e
        }catch (e: Exception) {
            log.error { "etfPriceList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun etfInfo(
        req: EtfInfoReq
    ): EtfInfoRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/etf")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka40002")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(EtfInfoRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw EtfInfoException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: EtfInfoException) {
            throw e
        }catch (e: Exception) {
            log.error { "etfInfo Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun etfTradeDailyList(
        req: EtfTradeDailyListReq
    ): EtfTradeDailyListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/etf")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka40008")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(EtfTradeDailyListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw EtfTradeDailyListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: EtfTradeDailyListException) {
            throw e
        }catch (e: Exception) {
            log.error { "etfTradeDaily Error" }

            throw RuntimeException(e.message)
        }
    }
}