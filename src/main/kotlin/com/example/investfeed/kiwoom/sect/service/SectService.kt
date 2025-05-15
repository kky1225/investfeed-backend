package com.example.investfeed.kiwoom.sect.service

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.SectCodeListException
import com.example.investfeed.kiwoom.exception.SectIndexListException
import com.example.investfeed.kiwoom.exception.SectInvestorException
import com.example.investfeed.kiwoom.exception.SectPriceException
import com.example.investfeed.kiwoom.exception.SectPriceNowException
import com.example.investfeed.kiwoom.sect.dto.req.SectCodeListReq
import com.example.investfeed.kiwoom.sect.dto.req.SectIndexListReq
import com.example.investfeed.kiwoom.sect.dto.req.SectInvestorReq
import com.example.investfeed.kiwoom.sect.dto.req.SectPriceNowReq
import com.example.investfeed.kiwoom.sect.dto.req.SectPriceReq
import com.example.investfeed.kiwoom.sect.dto.res.SectCodeListRes
import com.example.investfeed.kiwoom.sect.dto.res.SectIndexListRes
import com.example.investfeed.kiwoom.sect.dto.res.SectInvestorRes
import com.example.investfeed.kiwoom.sect.dto.res.SectPriceNowRes
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
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/sect")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10051")
                .bodyValue(req)
                .retrieve()
                .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                .bodyToMono(SectInvestorRes::class.java)
                .block()

            if (res?.return_code != 0) {
                throw SectInvestorException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: SectInvestorException) {
            throw e
        }catch (e: Exception) {
            log.error { "sectInvestor Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun sectPriceNow(
        req: SectPriceNowReq
    ): SectPriceNowRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/sect")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20001")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it -> it.isError }, { throw KiwoomApiException() })
                .bodyToMono(SectPriceNowRes::class.java)
                .block()

            log.info { "sectNowPriceRes $res" }

            if(res?.return_code != 0) {
                throw SectPriceNowException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: SectPriceNowException) {
            throw e
        }catch (e: Exception) {
            log.error { "sectNowPrice Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun sectPrice(
        req: SectPriceReq
    ): SectPriceRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/sect")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20002")
                .bodyValue(req)
                .retrieve()
                .onStatus({ t -> t.isError }, { throw KiwoomApiException() })
                .bodyToMono(SectPriceRes::class.java)
                .block()

            if (res?.return_code != 0) {
                throw SectPriceException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: SectPriceException) {
            throw e
        }catch (e: Exception) {
            log.error { "sectPrice Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun sectCodeList(
        req: SectCodeListReq
    ): SectCodeListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/stkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10101")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(SectCodeListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw SectCodeListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: SectCodeListException) {
            throw e
        }catch (e: Exception) {
            log.error { "sectCodeList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun sectIndexList(
        req: SectIndexListReq
    ): SectIndexListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/sect")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20003")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(SectIndexListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw SectIndexListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: SectIndexListException) {
            throw e
        }catch (e: Exception) {
            log.error { "sectIndexList Error" }

            throw RuntimeException(e.message)
        }
    }
}