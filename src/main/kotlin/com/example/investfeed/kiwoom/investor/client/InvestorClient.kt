package com.example.investfeed.kiwoom.investor.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.exception.*
import com.example.investfeed.kiwoom.investor.dto.req.InvestorTradeDailyReq
import com.example.investfeed.kiwoom.investor.dto.req.InvestorTradeOrganizeReq
import com.example.investfeed.kiwoom.investor.dto.req.InvestorTradeRankListReq
import com.example.investfeed.kiwoom.investor.dto.res.InvestorTradeDailyRes
import com.example.investfeed.kiwoom.investor.dto.res.InvestorTradeOrganizeRes
import com.example.investfeed.kiwoom.investor.dto.res.InvestorTradeRankListRes
import com.example.investfeed.kiwoom.investor.dto.res.KiwoomGoldInvestorRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class InvestorClient(
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String

    @KiwoomToken
    fun investorTradeDaily(
        req: InvestorTradeDailyReq
    ): InvestorTradeDailyRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header("Authorization", "Bearer $accessToken")
                .header("api-id", "ka10058")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(InvestorTradeDailyRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw InvestorTradeDailyException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: InvestorTradeDailyException) {
            throw e
        }catch (e: Exception) {
            log.error { "investorTradeDaily Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun investorTradeOrganize(
        req: InvestorTradeOrganizeReq
    ): InvestorTradeOrganizeRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header("Authorization", "Bearer $accessToken")
                .header("api-id", "ka10061")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(InvestorTradeOrganizeRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw InvestorTradeOrganizeException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: InvestorTradeOrganizeException) {
            throw e
        }catch (e: Exception) {
            log.error { "investorTradeOrganize Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun investorTradeRankList(
        req: InvestorTradeRankListReq
    ): InvestorTradeRankListRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/frgnistt")
                .header("Authorization", "Bearer $accessToken")
                .header("api-id", "ka10131")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(InvestorTradeRankListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw InvestorTradeRankListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: InvestorTradeRankListException) {
            throw e
        }catch (e: Exception) {
            log.error { "investorTradeRankList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun goldInvestor(): KiwoomGoldInvestorRes {
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
                .bodyToMono(KiwoomGoldInvestorRes::class.java)
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