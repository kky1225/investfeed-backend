package com.example.investfeed.kiwoom.rank.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.RankTradeDailyVolumeListException
import com.example.investfeed.kiwoom.exception.RankTradeVolumeListException
import com.example.investfeed.kiwoom.rank.dto.req.RankTradeDailyVolumeListReq
import com.example.investfeed.kiwoom.rank.dto.req.RankTradeVolumeListReq
import com.example.investfeed.kiwoom.rank.dto.res.RankTradeDailyVolumeListRes
import com.example.investfeed.kiwoom.rank.dto.res.RankTradeVolumeListRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class RankClient(
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String

    @KiwoomToken
    fun rankTradeVolumeList(
        req: RankTradeVolumeListReq
    ): RankTradeVolumeListRes? {
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
                .bodyToMono(RankTradeVolumeListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw RankTradeVolumeListException()
            }

            return res
        }catch(e: KiwoomApiException){
            throw e
        }catch(e: RankTradeVolumeListException){
            throw e
        }catch (e: Exception) {
            log.error { "rankTradeVolumeList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun rankTradeDailyVolumeList(
        req: RankTradeDailyVolumeListReq
    ): RankTradeDailyVolumeListRes? {
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
                .bodyToMono(RankTradeDailyVolumeListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw RankTradeDailyVolumeListException()
            }

            return res
        }catch(e: KiwoomApiException){
            throw e
        }catch(e: RankTradeDailyVolumeListException){
            throw e
        }catch (e: Exception) {
            log.error { "rankTradeDailyVolumeList Error" }

            throw RuntimeException(e.message)
        }
    }
}