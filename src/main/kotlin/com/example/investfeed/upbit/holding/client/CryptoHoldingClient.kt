package com.example.investfeed.upbit.holding.client

import com.example.investfeed.domain.auth.exception.InvalidApiKeyException
import com.example.investfeed.upbit.holding.dto.res.UpbitAccountRes
import io.jsonwebtoken.Jwts
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Service
class CryptoHoldingClient(
    @Qualifier("upbitWebClient")
    private val upbitWebClient: WebClient,
) {
    private val log = KotlinLogging.logger {}

    fun getAccounts(accessKey: String, secretKey: String): List<UpbitAccountRes> {
        try {
            val token = generateJwtToken(accessKey, secretKey)

            val res = upbitWebClient.get()
                .uri("/v1/accounts")
                .header("Authorization", "Bearer $token")
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<UpbitAccountRes>>() {})
                .block()

            return res ?: emptyList()
        } catch (e: Exception) {
            log.error { "upbit getAccounts Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    fun validateApiKey(accessKey: String, secretKey: String) {
        try {
            val token = generateJwtToken(accessKey, secretKey)
            upbitWebClient.get()
                .uri("/v1/api_keys")
                .header("Authorization", "Bearer $token")
                .retrieve()
                .bodyToMono<String>()
                .block()
        } catch (e: WebClientResponseException) {
            if (e.statusCode == HttpStatus.UNAUTHORIZED) {
                throw InvalidApiKeyException()
            }
            throw e
        }
    }

    private fun generateJwtToken(accessKey: String, secretKey: String): String {
        val key = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256")

        return Jwts.builder()
            .claim("access_key", accessKey)
            .claim("nonce", UUID.randomUUID().toString())
            .signWith(key, Jwts.SIG.HS256)
            .compact()
    }
}
