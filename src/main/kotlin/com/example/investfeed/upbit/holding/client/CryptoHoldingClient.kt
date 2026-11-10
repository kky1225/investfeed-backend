package com.example.investfeed.upbit.holding.client

import com.example.investfeed.common.exception.ExternalApiFailureException
import com.example.investfeed.common.exception.InvestFeedException
import com.example.investfeed.common.util.apiFailure
import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.domain.auth.exception.InvalidApiKeyException
import com.example.investfeed.upbit.exception.UpbitApiException
import com.example.investfeed.upbit.holding.dto.res.UpbitAccountRes
import io.jsonwebtoken.Jwts
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
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
                // 401 은 회원 API 키 문제라 오류가 아니다 (warn). 그 밖의 HTTP 오류만 error
                .onStatus({ it == HttpStatus.UNAUTHORIZED }, {
                    log.warn { "upbit getAccounts 401 Unauthorized (사용자 API 키 인증 실패)" }
                    throw InvalidApiKeyException()
                })
                .onStatus({ it.isError }, { res -> res.logHttpError("업비트"); throw UpbitApiException() })
                .bodyToMono(object : ParameterizedTypeReference<List<UpbitAccountRes>>() {})
                .block()

            return res ?: emptyList()
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "upbit getAccounts Error" }
            throw ExternalApiFailureException(e)
        }
    }

    fun validateApiKey(accessKey: String, secretKey: String) {
        try {
            val token = generateJwtToken(accessKey, secretKey)
            upbitWebClient.get()
                .uri("/v1/api_keys")
                .header("Authorization", "Bearer $token")
                .retrieve()
                // 401 은 회원이 잘못 입력한 키라 기록하지 않는다
                .onStatus({ it == HttpStatus.UNAUTHORIZED }, { throw InvalidApiKeyException() })
                .onStatus({ it.isError }, { res -> res.logHttpError("업비트"); throw UpbitApiException() })
                .bodyToMono<String>()
                .block()
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "upbit validateApiKey Error" }
            throw ExternalApiFailureException(e)
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
