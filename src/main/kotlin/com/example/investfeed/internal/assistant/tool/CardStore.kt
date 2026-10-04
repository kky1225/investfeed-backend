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

    private fun key(ref: String) = "${RedisKeyPrefix.ASSISTANT.prefix}CARD:$ref"
}
