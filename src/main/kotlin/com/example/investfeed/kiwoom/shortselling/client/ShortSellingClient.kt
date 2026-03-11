package com.example.investfeed.kiwoom.shortselling.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.ShortSellingException
import com.example.investfeed.kiwoom.shortselling.dto.req.KiwoomStockShortSellingReq
import com.example.investfeed.kiwoom.shortselling.dto.res.KiwoomStockShortSellingRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class ShortSellingClient(
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String
    private final val SHORT_SELLING_URL = "/api/dostk/shsa"

    @KiwoomToken
    fun stockShortSelling(
        req: KiwoomStockShortSellingReq
    ): KiwoomStockShortSellingRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + SHORT_SELLING_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10014")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomStockShortSellingRes>()
                .block()

            if (res?.return_code != 0) {
                throw ShortSellingException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: ShortSellingException) {
            throw e
        } catch (e: Exception) {
            log.error { "stockShortSelling Error" }

            throw RuntimeException(e.message)
        }
    }
}