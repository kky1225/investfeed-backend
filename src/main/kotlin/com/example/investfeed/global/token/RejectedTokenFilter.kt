package com.example.investfeed.global.token

import com.example.investfeed.common.util.failure
import com.example.investfeed.global.constant.RedisKeyPrefix
import mu.KotlinLogging
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration

/**
 * 증권사가 거부한 액세스 토큰을 Redis 에서 지워 다음 호출이 새 토큰을 받게 한다.
 *
 * 요청을 다시 보내지는 않는다. 화면은 프론트 재시도로, 스케줄러는 다음 실행으로 복구된다.
 * 재발급해도 같은 토큰이 돌아올 수 있어 키당 1분에 1번만 지운다.
 */
class RejectedTokenFilter private constructor(
    private val redisTemplate: StringRedisTemplate,
    private val label: String,
    private val tokenKeyPrefix: String,
    private val evictLockPrefix: String,
    private val shouldInspect: (HttpStatusCode) -> Boolean,
    private val isRejected: (ByteArray) -> Boolean,
) : ExchangeFilterFunction {
    private val log = KotlinLogging.logger {}

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        val authorization = request.headers().getFirst(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?: return next.exchange(request)

        return next.exchange(request).flatMap { response ->
            if (!shouldInspect(response.statusCode())) return@flatMap Mono.just(response)

            response.bodyToMono(ByteArray::class.java)
                .defaultIfEmpty(EMPTY_BODY)
                .map { body ->
                    if (isRejected(body)) evictAsync(authorization)
                    response.mutate()
                        .body { _: Flux<DataBuffer> -> Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body)) }
                        .build()
                }
        }
    }

    private fun evictAsync(authorization: String) {
        Schedulers.boundedElastic().schedule {
            runCatching { evict(authorization) }
                .onFailure { log.failure(it) { "$label 거부 토큰 삭제 실패" } }
        }
    }

    /** 이미 새 토큰으로 바뀐 키는 값이 달라 지우지 않는다 */
    internal fun evict(authorization: String) {
        val token = authorization.removePrefix(BEARER_PREFIX).trim()
        val ops = redisTemplate.opsForValue()
        redisTemplate.keys("$tokenKeyPrefix*")
            .filter { ops.get(it) == token }
            .forEach { key ->
                val lockKey = "$evictLockPrefix$EVICT_LOCK_INFIX${key.removePrefix(tokenKeyPrefix)}"
                if (ops.setIfAbsent(lockKey, "1", EVICT_COOLDOWN) == true) {
                    redisTemplate.delete(key)
                    log.error { "$label 토큰 거부 응답으로 저장된 토큰 삭제: key=$key" }
                }
            }
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val EVICT_LOCK_INFIX = "evict:"
        private const val TOSS_UNAUTHORIZED = 401
        private const val MAX_ERROR_BODY_BYTES = 2_048
        private val EVICT_COOLDOWN: Duration = Duration.ofMinutes(1)
        private val EMPTY_BODY = ByteArray(0)
        private val KIWOOM_TOKEN_REJECTED = Regex("\"return_msg\"\\s*:\\s*\"[^\"]*8005")
        private val TOSS_TOKEN_REJECTED = Regex("\"code\"\\s*:\\s*\"(expired-token|invalid-token|token-revoked)\"")

        /** 키움 명세에 8005 의 HTTP 상태 안내가 없어 모든 응답을 본다 */
        fun kiwoom(redisTemplate: StringRedisTemplate) = RejectedTokenFilter(
            redisTemplate = redisTemplate,
            label = "키움",
            tokenKeyPrefix = RedisKeyPrefix.KIWOOM_ACCESS_TOKEN.prefix,
            evictLockPrefix = RedisKeyPrefix.KIWOOM_ACCESS_TOKEN_LOCK.prefix,
            shouldInspect = { true },
            isRejected = ::isKiwoomTokenRejected,
        )

        fun toss(redisTemplate: StringRedisTemplate) = RejectedTokenFilter(
            redisTemplate = redisTemplate,
            label = "토스",
            tokenKeyPrefix = RedisKeyPrefix.TOSS_ACCESS_TOKEN.prefix,
            evictLockPrefix = RedisKeyPrefix.TOSS_ACCESS_TOKEN_LOCK.prefix,
            shouldInspect = { it.value() == TOSS_UNAUTHORIZED },
            isRejected = ::isTossTokenRejected,
        )

        internal fun isKiwoomTokenRejected(body: ByteArray): Boolean =
            body.size <= MAX_ERROR_BODY_BYTES && KIWOOM_TOKEN_REJECTED.containsMatchIn(String(body, Charsets.UTF_8))

        internal fun isTossTokenRejected(body: ByteArray): Boolean =
            TOSS_TOKEN_REJECTED.containsMatchIn(String(body, Charsets.UTF_8))
    }
}
