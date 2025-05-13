package com.example.investfeed.kiwoom.investor.service

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.exception.AccessTokenNotFound
import com.example.investfeed.kiwoom.exception.InvestorDailyTradeException
import com.example.investfeed.kiwoom.exception.InvestorOrganizeTradeException
import com.example.investfeed.kiwoom.exception.InvestorTradeRankException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.investor.dto.req.InvestorDailyTradeReq
import com.example.investfeed.kiwoom.investor.dto.req.InvestorTradeRankReq
import com.example.investfeed.kiwoom.investor.dto.req.InvestorOrganizeTradeReq
import com.example.investfeed.kiwoom.investor.dto.res.InvestorDailyTradeRes
import com.example.investfeed.kiwoom.investor.dto.res.InvestorTradeRankRes
import com.example.investfeed.kiwoom.investor.dto.res.InvestorOrganizeTradeRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class InvestorService(
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String

    @KiwoomToken
    fun investorDailyTrade(
        req: InvestorDailyTradeReq
    ): InvestorDailyTradeRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")

        accessToken ?: throw AccessTokenNotFound()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header("Authorization", "Bearer $accessToken")
                .header("api-key", "ka10058")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(InvestorDailyTradeRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw InvestorDailyTradeException()
            }

            return res
        }catch (e: Exception) {
            log.error { "investorDailyTrade Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun investorOrganizeTrade(
        req: InvestorOrganizeTradeReq
    ): InvestorOrganizeTradeRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")

        accessToken ?: throw AccessTokenNotFound()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header("Authorization", "Bearer $accessToken")
                .header("api-key", "ka10061")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(InvestorOrganizeTradeRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw InvestorOrganizeTradeException()
            }

            return res
        }catch (e: Exception) {
            log.error { "investorOrganizeTrade Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun investorTradeRank(
        req: InvestorTradeRankReq
    ): InvestorTradeRankRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")

        accessToken ?: throw AccessTokenNotFound()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/frgnistt")
                .header("Authorization", "Bearer $accessToken")
                .header("api-key", "ka10131")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(InvestorTradeRankRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw InvestorTradeRankException()
            }

            return res
        }catch (e: Exception) {
            log.error { "investorTradeRank Error" }

            throw RuntimeException(e.message)
        }
    }
}