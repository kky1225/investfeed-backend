package com.example.investfeed.kiwoom.rank.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.exception.*
import com.example.investfeed.kiwoom.rank.dto.req.KiwoomInvestorTradeDailyReq
import com.example.investfeed.kiwoom.rank.dto.req.KiwoomStockTradeValueListReq
import com.example.investfeed.kiwoom.rank.dto.req.KiwoomStockTradeVolumeListReq
import com.example.investfeed.kiwoom.rank.dto.req.KiwoomSurgeTradeVolumeListReq
import com.example.investfeed.kiwoom.rank.dto.res.KiwoomInvestorTradeDailyRes
import com.example.investfeed.kiwoom.rank.dto.res.KiwoomStockTradeValueListRes
import com.example.investfeed.kiwoom.rank.dto.res.KiwoomStockTradeVolumeListRes
import com.example.investfeed.kiwoom.rank.dto.res.KiwoomSurgeTradeVolumeListRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class RankClient(
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String
    private final val RANK_URL = "/api/dostk/rkinfo"

    @KiwoomToken
    fun stockTradeValueList(
        req: KiwoomStockTradeValueListReq
    ): KiwoomStockTradeValueListRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + RANK_URL)
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
                .uri(DEFAULT_URL + RANK_URL)
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
                .uri(DEFAULT_URL + RANK_URL)
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

    fun investorTradeDaily(
        req: KiwoomInvestorTradeDailyReq
    ): KiwoomInvestorTradeDailyRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + RANK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10065")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomInvestorTradeDailyRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw InvestorTradeDailyException()
            }

            return res
        }catch(e: KiwoomApiException){
            throw e
        }catch(e: InvestorTradeDailyException){
            throw e
        }catch (e: Exception) {
            log.error { "investorTradeDaily Error" }

            throw RuntimeException(e.message)
        }
    }
}