package com.example.investfeed.toss.auth.service

import com.example.investfeed.common.exception.ExternalApiFailureException
import com.example.investfeed.common.exception.InvestFeedException
import com.example.investfeed.common.util.apiFailure
import com.example.investfeed.common.util.failure
import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.domain.auth.exception.ApiKeyNotFoundException
import com.example.investfeed.domain.auth.exception.InvalidApiKeyException
import com.example.investfeed.domain.auth.repository.MemberApiKeyRepository
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.global.constant.RedisKeyPrefix
import com.example.investfeed.toss.auth.dto.res.TossAccessTokenRes
import com.example.investfeed.toss.exception.TossAccessTokenException
import com.example.investfeed.toss.exception.TossAccessTokenNotFoundException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.security.MessageDigest
import java.time.Duration
import java.util.Base64

/**
 * 토스증권 OpenAPI OAuth2(client_credentials) 토큰 발급/캐싱.
 *
 * 키움 [AuthClient] 와 동일한 회원별 토큰 캐싱 패턴을 사용한다.
 * - client_id → MemberApiKey.appKey, client_secret → MemberApiKey.secretKey
 * - 토큰은 발급자(회원) 본인 계좌만 접근하므로 회원별로 Redis 캐싱(TTL = expires_in).
 */
@Service
class TossAuthClient(
    @param:Value("\${toss.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("tossWebClient")
    private val tossWebClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>,
    private val memberApiKeyRepository: MemberApiKeyRepository,
    private val brokerRepository: BrokerRepository,
) {
    private val log = KotlinLogging.logger {}

    private val REDIS_KEY_PREFIX = RedisKeyPrefix.TOSS_ACCESS_TOKEN.prefix
    private val LOCK_KEY_PREFIX = RedisKeyPrefix.TOSS_ACCESS_TOKEN_LOCK.prefix

    companion object {
        private const val BROKER_NAME = "토스증권"
        private val DEFAULT_TTL: Duration = Duration.ofMinutes(30)
        private const val TTL_SAFETY_SECONDS = 60L
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
        val (clientId, _) = resolveApiKeys(loginId)
        val redisKey = getRedisKey(clientId)

        try {
            val accessToken = redisTemplate.opsForValue().get(redisKey)
            if (accessToken.isNullOrEmpty()) {
                refreshToken(loginId)
            }
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: Exception) {
            // Redis 오류 등 아직 기록되지 않은 예외만 남는다
            log.failure(e) { "tossAccessToken 실패 (loginId=$loginId)" }
            throw ExternalApiFailureException(e)
        }
    }

    fun getAccessToken(loginId: String): String {
        val (clientId, _) = resolveApiKeys(loginId)
        val redisKey = getRedisKey(clientId)
        return redisTemplate.opsForValue().get(redisKey)
            ?: throw TossAccessTokenNotFoundException()
    }

    private fun refreshToken(loginId: String) {
        val (clientId, clientSecret) = resolveApiKeys(loginId)
        val lockKey = getLockKey(clientId)
        val redisKey = getRedisKey(clientId)

        val isLocked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5))
        if (isLocked == false) {
            Thread.sleep(500)
            ensureAccessToken(loginId)
            return
        }

        try {
            val res = requestToken(clientId, clientSecret)

            val token = res?.access_token
            if (token.isNullOrBlank()) {
                log.error { "tossAccessToken failed: access_token 없음, loginId=$loginId" }
                throw TossAccessTokenException()
            }

            val ttl = res.expires_in
                ?.let { Duration.ofSeconds((it - TTL_SAFETY_SECONDS).coerceAtLeast(60)) }
                ?: DEFAULT_TTL
            redisTemplate.opsForValue().set(redisKey, token, ttl)
            log.info { "토스 토큰 재발급: loginId=$loginId, redisKey=$redisKey, TTL ${ttl.toMinutes()}분" }
        } catch (e: TossTokenRejectedException) {
            // body·Basic 두 방식 모두 거부 → 등록된 키가 더 이상 유효하지 않다
            log.error { "tossRefreshToken 거부 (loginId=$loginId): status=${e.status}" }
            throw TossAccessTokenException()
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "tossRefreshToken 실패 (loginId=$loginId)" }
            throw ExternalApiFailureException(e)
        }
    }

    fun validateApiKey(clientId: String, clientSecret: String) {
        try {
            val res = requestToken(clientId, clientSecret)

            if (res?.access_token.isNullOrBlank()) {
                log.warn { "토스 키 검증 실패: 200 응답이지만 access_token 미발급" }
                throw InvalidApiKeyException()
            }
        } catch (e: TossTokenRejectedException) {
            // 회원이 잘못 입력한 키라 warn
            log.warn { "토스 키 검증 실패: status=${e.status}" }
            throw InvalidApiKeyException()
        } catch (e: InvestFeedException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "토스 키 검증 실패" }
            throw ExternalApiFailureException(e)
        }
    }

    /** 키 거부(401·400). 방식을 바꿔 다시 시도하거나 호출부가 키 문제로 처리하므로 여기서는 기록하지 않는다 */
    private class TossTokenRejectedException(val status: Int) : RuntimeException("status=$status")

    private fun requestToken(clientId: String, clientSecret: String): TossAccessTokenRes? {
        return try {
            postToken(clientId, clientSecret, useBasic = false)
        } catch (e: TossTokenRejectedException) {
            log.warn { "토스 토큰 body 방식 실패(${e.status}), Basic 방식 재시도" }
            postToken(clientId, clientSecret, useBasic = true)
        }
    }

    private fun postToken(clientId: String, clientSecret: String, useBasic: Boolean): TossAccessTokenRes? {
        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "client_credentials")
            if (!useBasic) {
                add("client_id", clientId)
                add("client_secret", clientSecret)
            }
        }

        val spec = tossWebClient.post()
            .uri("$DEFAULT_URL/oauth2/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        if (useBasic) {
            spec.header(HttpHeaders.AUTHORIZATION, basicAuth(clientId, clientSecret))
        }

        return spec.body(BodyInserters.fromFormData(form))
            .retrieve()
            .onStatus({ it == HttpStatus.UNAUTHORIZED || it == HttpStatus.BAD_REQUEST }, { res ->
                throw TossTokenRejectedException(res.statusCode().value())
            })
            .onStatus({ it.isError }, { res -> res.logHttpError("토스"); throw TossAccessTokenException() })
            .bodyToMono<TossAccessTokenRes>()
            .block()
    }

    private fun basicAuth(clientId: String, clientSecret: String): String {
        val encoded = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray(Charsets.UTF_8))
        return "Basic $encoded"
    }

    fun hasApiKey(loginId: String): Boolean {
        val broker = brokerRepository.findByName(BROKER_NAME) ?: return false
        return memberApiKeyRepository.findByMemberLoginIdAndBrokerId(loginId, broker.id) != null
    }

    fun currentLoginIdOrNull(): String? = runCatching { getLoginIdFromSecurityContext() }.getOrNull()

    private fun resolveApiKeys(loginId: String): Pair<String, String> {
        val broker = brokerRepository.findByName(BROKER_NAME)
            ?: throw ApiKeyNotFoundException()

        val memberApiKey = memberApiKeyRepository.findByMemberLoginIdAndBrokerId(loginId, broker.id)
            ?: throw ApiKeyNotFoundException()

        return Pair(memberApiKey.appKey, memberApiKey.secretKey)
    }

    private fun getRedisKey(clientId: String): String = "$REDIS_KEY_PREFIX${shortHash(clientId)}"

    private fun getLockKey(clientId: String): String = "$LOCK_KEY_PREFIX${shortHash(clientId)}"

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
