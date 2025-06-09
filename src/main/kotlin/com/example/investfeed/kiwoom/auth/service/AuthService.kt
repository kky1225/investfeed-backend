package com.example.investfeed.kiwoom.auth.service

import com.example.investfeed.kiwoom.auth.dto.req.AccessTokenReq
import com.example.investfeed.kiwoom.auth.dto.res.AccessTokenRes
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Service
class AuthService(
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.app-key}")
    private lateinit var APP_KEY: String

    @Value("\${kiwoom.secret-key}")
    private lateinit var SECRET_KEY: String

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String

    fun accessToken() {
        log.info { "accessToken" }

        try {
            val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")

            if(accessToken.isNullOrEmpty()) {
                refreshToken()
            }
        }catch (e: Exception) {
            log.error { "accessToken Error" }

            throw RuntimeException(e.message)
        }
    }

    private fun refreshToken() {
        log.info { "refreshToken" }

        try {
            val accessTokenRes = webClient.post()
                .uri("$DEFAULT_URL/oauth2/token")
                .bodyValue(
                    AccessTokenReq(
                        appkey = APP_KEY,
                        secretkey = SECRET_KEY
                    )
                )
                .retrieve()
                .onStatus({ it -> it.isError }, { throw KiwoomApiException() })
                .bodyToMono(AccessTokenRes::class.java)
                .block()

            if(accessTokenRes?.return_code != 0) {
                throw RuntimeException("access token 오류")
            }

            accessTokenRes.token?.let { redisTemplate.opsForValue().set("kiwoom:access_token", it, Duration.ofMinutes(30)) }
        }catch (e: Exception) {
            log.error { "refreshToken Error" }

            throw RuntimeException(e.message)
        }
    }
}