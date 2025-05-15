package com.example.investfeed.kiwoom.stock.service

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.StockInfoTradeDailyException
import com.example.investfeed.kiwoom.exception.StockInfoException
import com.example.investfeed.kiwoom.exception.StockInfoJumpListException
import com.example.investfeed.kiwoom.exception.StockInfoListException
import com.example.investfeed.kiwoom.stock.dto.req.StockInfoTradeDailyReq
import com.example.investfeed.kiwoom.stock.dto.req.StockInfoJumpListReq
import com.example.investfeed.kiwoom.stock.dto.req.StockInfoListReq
import com.example.investfeed.kiwoom.stock.dto.req.StockInfoReq
import com.example.investfeed.kiwoom.stock.dto.res.StockInfoTradeDailyRes
import com.example.investfeed.kiwoom.stock.dto.res.StockInfoJumpListRes
import com.example.investfeed.kiwoom.stock.dto.res.StockInfoListRes
import com.example.investfeed.kiwoom.stock.dto.res.StockInfoRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class StockInfoService(
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String

    @KiwoomToken
    fun stockInfoList(
        req: StockInfoListReq
    ): StockInfoListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10099")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono(StockInfoListRes::class.java)
                .block()

            if (res?.return_code != 0) {
                throw StockInfoListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockInfoListException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockInfoList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockInfo(
        stockNm: String
    ): StockInfoRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10001")
                .bodyValue(
                    StockInfoReq(
                        stk_cd = stockNm
                    )
                )
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono(StockInfoRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockInfoException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockInfoException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockInfo Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockInfoTradeDaily(
        req: StockInfoTradeDailyReq
    ): StockInfoTradeDailyRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10015")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono(StockInfoTradeDailyRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockInfoTradeDailyException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockInfoTradeDailyException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockInfoTradeDaily Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockInfoJumpList(
        req: StockInfoJumpListReq
    ): StockInfoJumpListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10019")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono(StockInfoJumpListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockInfoJumpListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockInfoJumpListException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockInfoJumpList Error" }

            throw RuntimeException(e.message)
        }
    }
}