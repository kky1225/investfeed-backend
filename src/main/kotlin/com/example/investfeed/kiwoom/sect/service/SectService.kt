package com.example.investfeed.kiwoom.sect.service

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthService
import com.example.investfeed.kiwoom.sect.dto.req.SectInvestorReq
import com.example.investfeed.kiwoom.sect.dto.req.SectPriceReq
import com.example.investfeed.kiwoom.sect.dto.res.SectInvestorRes
import com.example.investfeed.kiwoom.sect.dto.res.SectPriceRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class SectService(
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String

    @KiwoomToken
    fun sectInvestor(
        req: SectInvestorReq
    ): SectInvestorRes? {
        try {
            val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")

            accessToken ?: throw RuntimeException("access token is null")

            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/sect")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10051")
                .bodyValue(req)
                .retrieve()
                .onStatus( { t -> t.isError }, { throw RuntimeException("통신 오류") })
                .bodyToMono(SectInvestorRes::class.java)
                .block()

            if (res?.return_code != 0) {
                throw RuntimeException("업종별 투자자 순매수 조회 실패")
            }

            return res
        }catch (e: Exception) {
            log.error { "sectInvestor error $e" }

            throw RuntimeException(e.message)
        }
    }

    fun sectPrice(
        req: SectPriceReq
    ): SectPriceRes? {
        try {
            val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")

            accessToken ?: throw RuntimeException("access token is null")

            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/sect")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20002")
                .bodyValue(req)
                .retrieve()
                .onStatus({ t -> t.isError }, { throw RuntimeException("통신 오류") })
                .bodyToMono(SectPriceRes::class.java)
                .block()

            if (res?.return_code != 0) {
                throw RuntimeException("업종별 주가 조회 실패")
            }

            return res
        }catch (e: Exception) {
            log.error { "sectPrice error $e" }

            throw RuntimeException(e.message)
        }
    }
}