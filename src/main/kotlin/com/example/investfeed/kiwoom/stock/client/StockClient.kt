package com.example.investfeed.kiwoom.stock.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.exception.*
import com.example.investfeed.kiwoom.stock.dto.req.*
import com.example.investfeed.kiwoom.stock.dto.res.*
import com.example.investfeed.kiwoom.stock.entity.req.*
import com.example.investfeed.kiwoom.stock.entity.res.*
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
    fun stockDefaultInfo(
        req: KiwoomDefaultStockInfoReq
    ): KiwoomStockDefaultInfoRes {
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
                .bodyToMono(KiwoomStockDefaultInfoRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockDefaultInfoException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockDefaultInfoException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockInfo Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockInfo(
        req: KiwoomStockInfoReq
    ): KiwoomStockInfoRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10100")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockInfoRes::class.java)
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
    fun stockTradeInfo(
        req: KiwoomStockTradeInfoReq
    ): KiwoomStockTradeInfoRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/mrkcond")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10006")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockTradeInfoRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockTradeInfoException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: StockTradeInfoException) {
            throw e
        } catch (e: Exception) {
            log.error { "stockTradeInfo Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockInvestor(
        req: KiwoomStockInvestorReq
    ): KiwoomStockInvestorRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10059")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockInvestorRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockInvestorException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockInvestorException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockInvestor Error" }

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

    @KiwoomToken
    fun stockSurgeTradeVolumeList(
        req: KiwoomSurgeTradeVolumeListReq
    ): KiwoomSurgeTradeVolumeListRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/rkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10023")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomSurgeTradeVolumeListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockSurgeTradeVolumeListException()
            }

            return res
        }catch(e: KiwoomApiException){
            throw e
        }catch(e: StockSurgeTradeVolumeListException){
            throw e
        }catch (e: Exception) {
            log.error { "stockSurgeTradeVolumeList Error" }

            throw RuntimeException(e.message)
        }
    }
}