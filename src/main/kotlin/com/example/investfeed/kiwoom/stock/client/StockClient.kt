package com.example.investfeed.kiwoom.stock.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.StockInfoException
import com.example.investfeed.kiwoom.exception.StockInfoListException
import com.example.investfeed.kiwoom.exception.StockJumpListException
import com.example.investfeed.kiwoom.exception.StockNewPriceListException
import com.example.investfeed.kiwoom.exception.StockSinglePriceListException
import com.example.investfeed.kiwoom.exception.StockTradeDailyListException
import com.example.investfeed.kiwoom.exception.StockTradeValueListException
import com.example.investfeed.kiwoom.exception.StockTradeVolumeListException
import com.example.investfeed.kiwoom.stock.dto.req.StockInfoListReq
import com.example.investfeed.kiwoom.stock.dto.req.StockInfoReq
import com.example.investfeed.kiwoom.stock.dto.req.StockJumpListReq
import com.example.investfeed.kiwoom.stock.dto.req.StockNewPriceListReq
import com.example.investfeed.kiwoom.stock.dto.req.StockSinglePriceListReq
import com.example.investfeed.kiwoom.stock.dto.req.StockTradeDailyListReq
import com.example.investfeed.kiwoom.stock.entity.req.KiwoomStockTradeValueReq
import com.example.investfeed.kiwoom.stock.dto.res.StockInfoListRes
import com.example.investfeed.kiwoom.stock.dto.res.StockInfoRes
import com.example.investfeed.kiwoom.stock.dto.res.StockJumpListRes
import com.example.investfeed.kiwoom.stock.dto.res.StockNewPriceListRes
import com.example.investfeed.kiwoom.stock.dto.res.StockSinglePriceListRes
import com.example.investfeed.kiwoom.stock.dto.res.StockTradeDailyListRes
import com.example.investfeed.kiwoom.stock.entity.req.KiwoomStockTradeVolumeListReq
import com.example.investfeed.kiwoom.stock.entity.res.KiwoomStockTradeValueListRes
import com.example.investfeed.kiwoom.stock.entity.res.KiwoomStockTradeVolumeListRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class StockClient(
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
        req: StockInfoReq
    ): StockInfoRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10001")
                .bodyValue(req)
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
    fun stockTradeDailyList(
        req: StockTradeDailyListReq
    ): StockTradeDailyListRes? {
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
                .bodyToMono(StockTradeDailyListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockTradeDailyListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockTradeDailyListException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockTradeDailyList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockJumpList(
        req: StockJumpListReq
    ): StockJumpListRes? {
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
                .bodyToMono(StockJumpListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockJumpListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockJumpListException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockJumpList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockSinglePriceList(
        req: StockSinglePriceListReq
    ): StockSinglePriceListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/mrkcond")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10087")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono(StockSinglePriceListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockSinglePriceListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockSinglePriceListException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockSinglePriceList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockNewPriceList(
        req: StockNewPriceListReq
    ): StockNewPriceListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10016")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono(StockNewPriceListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockNewPriceListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockNewPriceListException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockNewPriceList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockTradeValueList(
        req: KiwoomStockTradeValueReq
    ): KiwoomStockTradeValueListRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/rkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10032")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockTradeValueListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockTradeValueListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockTradeValueListException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockTradeValueList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockTradeVolumeList(
        req: KiwoomStockTradeVolumeListReq
    ): KiwoomStockTradeVolumeListRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/rkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10030")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockTradeVolumeListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockTradeVolumeListException()
            }

            return res
        }catch(e: KiwoomApiException){
            throw e
        }catch(e: StockTradeVolumeListException){
            throw e
        }catch (e: Exception) {
            log.error { "stockTradeVolumeList Error" }

            throw RuntimeException(e.message)
        }
    }
}