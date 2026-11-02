package com.example.investfeed.internal.assistant.tool

import com.example.investfeed.global.constant.RedisKeyPrefix
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime

@Service
class CardStore(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        val TTL: Duration = Duration.ofMinutes(30)
        private const val HEX = "0123456789abcdef"
    }

    private val random = SecureRandom()

    data class StoredCard(
        val ref: String,
        val memberId: Long,
        val kind: String,
        val createdAt: LocalDateTime,
        val payload: Any,
    )

    fun put(memberId: Long, kind: String, payload: Any): CardRef {
        val ref = "c_" + (1..12).map { HEX[random.nextInt(16)] }.joinToString("")
        val card = StoredCard(ref, memberId, kind, LocalDateTime.now(), payload)
        redisTemplate.opsForValue().set(key(ref), objectMapper.writeValueAsString(card), TTL)
        return CardRef(ref, kind)
    }

    fun get(memberId: Long, ref: String): StoredCard? =
        redisTemplate.opsForValue().get(key(ref))
            ?.let { objectMapper.readValue(it, StoredCard::class.java) }
            ?.takeIf { it.memberId == memberId }

    fun delete(ref: String) {
        redisTemplate.delete(key(ref))
    }

    /** 회원별로 아직 처리하지 않은 확인 카드 하나를 가리킨다 — 이어서 말하면 이 카드를 고친다 (TTL 은 카드와 같다) */
    fun setPending(memberId: Long, kind: String, ref: String) {
        redisTemplate.opsForValue().set(pendingKey(memberId, kind), ref, TTL)
    }

    fun getPending(memberId: Long, kind: String): StoredCard? =
        redisTemplate.opsForValue().get(pendingKey(memberId, kind))
            ?.let { get(memberId, it) }
            ?.takeIf { it.kind == kind }

    /** 가리키는 카드가 ref 일 때만 지운다 (그 사이 새 카드로 바뀌었으면 그대로 둔다) */
    fun clearPending(memberId: Long, kind: String, ref: String) {
        val key = pendingKey(memberId, kind)
        if (redisTemplate.opsForValue().get(key) == ref) redisTemplate.delete(key)
    }

    private fun key(ref: String) = "${RedisKeyPrefix.ASSISTANT.prefix}CARD:$ref"

    private fun pendingKey(memberId: Long, kind: String) = "${RedisKeyPrefix.ASSISTANT.prefix}CARD_PENDING:$kind:$memberId"
}
