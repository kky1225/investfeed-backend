package com.example.investfeed.kiwoom.auth.service

import com.example.investfeed.domain.auth.repository.MemberApiKeyRepository
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.kiwoom.auth.dto.req.AccessTokenReq
import com.example.investfeed.kiwoom.auth.dto.res.AccessTokenRes
import com.example.investfeed.domain.auth.exception.ApiKeyNotFoundException
import com.example.investfeed.domain.auth.exception.InvalidApiKeyException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import com.example.investfeed.global.constant.RedisKeyPrefix
import com.example.investfeed.kiwoom.exception.AccessTokenException
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
import java.security.MessageDigest
import java.time.Duration

@Service
class AuthClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @param:Value("\${kiwoom.mock-url}")
    private val MOCK_URL: String,
    @param:Value("\${kiwoom.mock-appkey:}")
    private val MOCK_APPKEY: String,
    @param:Value("\${kiwoom.mock-secretkey:}")
    private val MOCK_SECRETKEY: String,
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
        val (appKey, _) = resolveApiKeys(loginId)
        val redisKey = getRedisKey(appKey)

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
        val (appKey, _) = resolveApiKeys(loginId)
        val redisKey = getRedisKey(appKey)
        return redisTemplate.opsForValue().get(redisKey)
            ?: throw AccessTokenNotFoundException()
    }

    private fun refreshToken(loginId: String) {
        val (appKey, secretKey) = resolveApiKeys(loginId)
        val lockKey = getLockKey(appKey)
        val redisKey = getRedisKey(appKey)

        log.info { "refreshToken [loginId=$loginId, redisKey=$redisKey]" }

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
                log.warn { "accessToken failed: return_code=${accessTokenRes?.return_code}, return_msg=${accessTokenRes?.return_msg}, loginId=$loginId" }
                throw AccessTokenException()
            }

            accessTokenRes.token?.let {
                redisTemplate.opsForValue().set(redisKey, it, Duration.ofMinutes(30))
            }
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: AccessTokenException) {
            throw e
        } catch (e: Exception) {
            log.warn { "refreshToken 실패 (loginId=$loginId): ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    // ── 모의투자 전용 토큰 ────────────────────────────────────────────────────
    // 실거래와 동일 appkey/secret 으로 **모의 도메인(kiwoom.mock-url)** 에서 토큰 발급.
    // MOCK_APPKEY 는 application-local.yml 공통값이라 사용자별 분리 의미 없음 → 단일 키로 통일.
    // MockAccountClient / KiwoomOrderClient 가 @KiwoomMockToken 으로 사용.

    fun accessTokenMock() {
        val loginId = getLoginIdFromSecurityContext()
        ensureAccessTokenMock(loginId)
    }

    fun getCurrentAccessTokenMock(): String {
        return redisTemplate.opsForValue().get(getMockRedisKey())
            ?: throw AccessTokenNotFoundException()
    }

    fun ensureAccessTokenMock(loginId: String) {
        try {
            val accessToken = redisTemplate.opsForValue().get(getMockRedisKey())
            if (accessToken.isNullOrEmpty()) {
                refreshTokenMock(loginId)
            }
        } catch (e: Exception) {
            log.warn { "accessTokenMock 실패 (loginId=$loginId): ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    private fun refreshTokenMock(loginId: String) {
        val lockKey = getMockLockKey()
        val redisKey = getMockRedisKey()
        log.info { "refreshTokenMock [loginId=$loginId, redisKey=$redisKey]" }

        val isLocked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5))
        if (isLocked == false) {
            Thread.sleep(500)
            ensureAccessTokenMock(loginId)
            return
        }

        // 모의투자 전용 키(실전 키와 별개). 미설정 시 명확한 에러로 조기 실패.
        if (MOCK_APPKEY.isBlank() || MOCK_SECRETKEY.isBlank()) {
            throw RuntimeException(
                "모의투자 appkey/secret 미설정 — application-local.yml 의 kiwoom.mock-appkey/mock-secretkey 를 채우세요."
            )
        }
        val appKey = MOCK_APPKEY
        val secretKey = MOCK_SECRETKEY

        try {
            val accessTokenRes = kiwoomWebClient.post()
                .uri("$MOCK_URL/oauth2/token")
                .bodyValue(AccessTokenReq(appkey = appKey, secretkey = secretKey))
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<AccessTokenRes>()
                .block()

            if (accessTokenRes?.return_code != 0) {
                log.warn { "accessTokenMock failed: return_code=${accessTokenRes?.return_code}, return_msg=${accessTokenRes?.return_msg}, loginId=$loginId" }
                throw AccessTokenException()
            }

            accessTokenRes.token?.let {
                redisTemplate.opsForValue().set(redisKey, it, Duration.ofMinutes(30))
            }
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: AccessTokenException) {
            throw e
        } catch (e: Exception) {
            log.warn { "refreshTokenMock 실패 (loginId=$loginId): ${e.message}" }
            throw RuntimeException(e.message)
        }
    }

    // 모의 토큰은 MOCK_APPKEY 공통값이라 사용자별 분리 없음 → 단일 redis 키.
    private fun getMockRedisKey(): String = "${REDIS_KEY_PREFIX}mock"

    private fun getMockLockKey(): String = "${LOCK_KEY_PREFIX}mock"

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

    // 같은 키움 키를 여러 계정이 공유하는 경우(예: super 운영 + 본인 사용자 계정) 토큰 공유로 충돌 회피.
    // appkey 를 그대로 redis 키에 노출하지 않고 SHA-256 앞 16자(hex) 해시 사용.
    private fun getRedisKey(appKey: String): String {
        return "$REDIS_KEY_PREFIX${shortHash(appKey)}"
    }

    private fun getLockKey(appKey: String): String {
        return "$LOCK_KEY_PREFIX${shortHash(appKey)}"
    }

    private fun shortHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    private fun getLoginIdFromSecurityContext(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.isAuthenticated && authentication.name != "anonymousUser") {
            return authentication.name
        }
        throw AuthenticationCredentialsNotFoundException("인증이 필요합니다.")
    }
}
