package com.example.investfeed.kiwoom.stockinfo.service

import com.example.investfeed.kiwoom.stockinfo.dto.req.StockInfoDailyTradeReq
import com.example.investfeed.kiwoom.stockinfo.dto.req.StockInfoJumpListReq
import com.example.investfeed.kiwoom.stockinfo.dto.req.StockInfoListReq
import com.example.investfeed.kiwoom.stockinfo.dto.req.StockInfoReq
import com.example.investfeed.kiwoom.stockinfo.dto.res.StockInfoDailyTradeRes
import com.example.investfeed.kiwoom.stockinfo.dto.res.StockInfoJumpRes
import com.example.investfeed.kiwoom.stockinfo.dto.res.StockInfoListRes
import com.example.investfeed.kiwoom.stockinfo.dto.res.StockInfoRes
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

    fun stockInfoList(
        req: StockInfoListReq
    ): StockInfoListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")

        accessToken ?: throw RuntimeException("access token is null")

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10099")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw RuntimeException("통신 오류") })
                .bodyToMono(StockInfoListRes::class.java)
                .block()

            if (res?.return_code != 0) {
                throw RuntimeException("정목 정보 리스트 조회 실패")
            }

            return res
        }catch (e: Exception) {
            log.error { "stockInfoList Error" }

            throw RuntimeException(e.message)
        }
    }

    fun stockInfo(
        req: StockInfoReq
    ): StockInfoRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")

        accessToken ?: throw RuntimeException("access token is null")

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10001")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw RuntimeException("통신 오류") })
                .bodyToMono(StockInfoRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw RuntimeException("주식 기본 정보 조회 실패")
            }

            return res
        }catch (e: Exception) {
            log.error { "stockInfo Error" }

            throw RuntimeException(e.message)
        }
    }

    fun stockInfoDailyTrade(
        req: StockInfoDailyTradeReq
    ): StockInfoDailyTradeRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")

        accessToken ?: throw RuntimeException("access token is null")

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10015")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw RuntimeException("통신 오류") })
                .bodyToMono(StockInfoDailyTradeRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw RuntimeException("일별 거래 상세 조회 실패")
            }

            return res
        }catch (e: Exception) {
            log.error { "stockInfoDailyTrade Error" }

            throw RuntimeException(e.message)
        }
    }

    fun stockInfoJumpList(
        req: StockInfoJumpListReq
    ): StockInfoJumpRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")

        accessToken ?: throw RuntimeException("access token is null")

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10019")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw RuntimeException("통신 오류") })
                .bodyToMono(StockInfoJumpRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw RuntimeException("가격 급등락 조회 실패")
            }

            return res
        }catch (e: Exception) {
            log.error { "stockInfoJumpList Error" }

            throw RuntimeException(e.message)
        }
    }
}