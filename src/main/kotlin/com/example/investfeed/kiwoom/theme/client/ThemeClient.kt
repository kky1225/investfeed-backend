package com.example.investfeed.kiwoom.theme.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.ThemeGroupListException
import com.example.investfeed.kiwoom.exception.ThemeGroupStockListException
import com.example.investfeed.kiwoom.theme.dto.req.KiwoomThemeGroupReq
import com.example.investfeed.kiwoom.theme.dto.req.KiwoomThemeGroupStockReq
import com.example.investfeed.kiwoom.theme.dto.res.KiwoomThemeGroupRes
import com.example.investfeed.kiwoom.theme.dto.res.KiwoomThemeGroupStockRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class ThemeClient(
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String

    @KiwoomToken
    fun themeGroupList(
        req: KiwoomThemeGroupReq
    ): KiwoomThemeGroupRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/thme")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka90001")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomThemeGroupRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw ThemeGroupListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: ThemeGroupListException) {
            throw e
        }catch (e: Exception) {
            log.error { "themeGroupList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun themeGroupStockList(
        req: KiwoomThemeGroupStockReq
    ): KiwoomThemeGroupStockRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/thme")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka90002")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomThemeGroupStockRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw ThemeGroupStockListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: ThemeGroupStockListException) {
            throw e
        }catch (e: Exception) {
            log.error { "themeGroupStockList Error" }

            throw RuntimeException(e.message)
        }
    }
}