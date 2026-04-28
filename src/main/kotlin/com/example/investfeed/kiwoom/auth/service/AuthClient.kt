package com.example.investfeed.kiwoom.auth.service

import com.example.investfeed.domain.auth.repository.MemberApiKeyRepository
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.kiwoom.auth.dto.req.AccessTokenReq
import com.example.investfeed.kiwoom.auth.dto.res.AccessTokenRes
import com.example.investfeed.domain.auth.exception.ApiKeyNotFoundException
import com.example.investfeed.domain.auth.exception.InvalidApiKeyException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import com.example.investfeed.global.constant.RedisKeyPrefix
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Duration

@Service
class AuthClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>,
    private val memberApiKeyRepository: MemberApiKeyRepository,
    private val brokerRepository: BrokerRepository
) {
    private val log = KotlinLogging.logger {}

    private val REDIS_KEY_PREFIX = RedisKeyPrefix.KIWOOM_ACCESS_TOKEN.prefix
    private val LOCK_KEY_PREFIX = RedisKeyPrefix.KIWOOM_ACCESS_TOKEN_LOCK.prefix

    companion object {
        private const val BROKER_NAME = "키움증권"
    }

    fun accessToken() {
        val loginId = getLoginIdFromSecurityContext()
        ensureAccessToken(loginId)
    }

    fun getCurrentAccessToken(): String {
        val loginId = getLoginIdFromSecurityContext()
        return getAccessToken(loginId)
    }

    fun ensureAccessToken(loginId: String) {
        val redisKey = getRedisKey(loginId)

        log.info { "accessToken [loginId=$loginId, redisKey=$redisKey]" }

        try {
            val accessToken = redisTemplate.opsForValue().get(redisKey)

            if (accessToken.isNullOrEmpty()) {
                refreshToken(loginId)
            }
        } catch (e: Exception) {
            log.warn { "accessToken 실패 (loginId=$loginId): ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    fun getAccessToken(loginId: String): String {
        val redisKey = getRedisKey(loginId)
        return redisTemplate.opsForValue().get(redisKey)
            ?: throw AccessTokenNotFoundException()
    }

    private fun refreshToken(loginId: String) {
        val lockKey = getLockKey(loginId)

        log.info { "refreshToken [loginId=$loginId]" }

        val isLocked = redisTemplate.opsForValue().setIfAbsent(
            lockKey,
            "1",
            Duration.ofSeconds(5)
        )

        if (isLocked == false) {
            Thread.sleep(500)
            ensureAccessToken(loginId)
            return
        }

        val (appKey, secretKey) = resolveApiKeys(loginId)
        val redisKey = getRedisKey(loginId)

        try {
            val accessTokenRes = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/oauth2/token")
                .bodyValue(
                    AccessTokenReq(
                        appkey = appKey,
                        secretkey = secretKey
                    )
                )
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<AccessTokenRes>()
                .block()

            if (accessTokenRes?.return_code != 0) {
                throw RuntimeException("access token 오류: return_code=${accessTokenRes?.return_code}, return_msg=${accessTokenRes?.return_msg}")
            }

            accessTokenRes.token?.let {
                redisTemplate.opsForValue().set(redisKey, it, Duration.ofMinutes(30))
            }
        } catch (e: Exception) {
            log.warn { "refreshToken 실패 (loginId=$loginId): ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    fun validateApiKey(appKey: String, secretKey: String) {
        val res = kiwoomWebClient.post()
            .uri("$DEFAULT_URL/oauth2/token")
            .bodyValue(AccessTokenReq(appkey = appKey, secretkey = secretKey))
            .retrieve()
            .bodyToMono<AccessTokenRes>()
            .block()

        if (res?.return_code != 0) {
            throw InvalidApiKeyException()
        }
    }

    private fun resolveApiKeys(loginId: String): Pair<String, String> {
        val broker = brokerRepository.findByName(BROKER_NAME)
            ?: throw ApiKeyNotFoundException()

        val memberApiKey = memberApiKeyRepository.findByMemberLoginIdAndBrokerId(loginId, broker.id)
            ?: throw ApiKeyNotFoundException()

        return Pair(memberApiKey.appKey, memberApiKey.secretKey)
    }

    private fun getRedisKey(loginId: String): String {
        return "$REDIS_KEY_PREFIX$loginId"
    }

    private fun getLockKey(loginId: String): String {
        return "$LOCK_KEY_PREFIX$loginId"
    }

    private fun getLoginIdFromSecurityContext(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.isAuthenticated && authentication.name != "anonymousUser") {
            return authentication.name
        }
        throw AuthenticationCredentialsNotFoundException("인증이 필요합니다.")
    }
}
