package com.example.investfeed.kiwoom.auth.service

import com.example.investfeed.kiwoom.auth.model.AccessTokenReq
import com.example.investfeed.kiwoom.auth.model.AccessTokenRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class AuthService(
    private val webClient: WebClient
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.app-key}")
    private lateinit var APP_KEY: String

    @Value("\${kiwoom.secret-key}")
    private lateinit var SECRET_KEY: String

    fun accessToken(): AccessTokenRes? {
        try {
            return webClient.post()
                .uri("https://api.kiwoom.com/oauth2/token")
                .bodyValue(
                    AccessTokenReq(
                        appkey = APP_KEY,
                        secretkey = SECRET_KEY
                    )
                )
                .retrieve()
                .onStatus({ t -> t.isError }, { throw RuntimeException("통신 오류") })
                .bodyToMono(AccessTokenRes::class.java)
                .block()
        }catch (e: RuntimeException) {
            log.error { "accessToken error" }

            throw RuntimeException(e.message)
        }
    }
}