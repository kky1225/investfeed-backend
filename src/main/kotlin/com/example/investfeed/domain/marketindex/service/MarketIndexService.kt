package com.example.investfeed.domain.marketindex.service

import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.dto.res.MarketIndexRes
import com.example.investfeed.global.constant.RedisKeyPrefix
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class MarketIndexService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    private val KEY_PREFIX = RedisKeyPrefix.MARKET_INDEX.prefix

    companion object {
        private val TTL = Duration.ofDays(1)
        private val STALE_AFTER = Duration.ofMinutes(10)
    }

    fun saveAll(list: List<MarketIndexRes>) {
        list.forEach { item ->
            redisTemplate.opsForValue().set(
                "$KEY_PREFIX${item.type}",
                objectMapper.writeValueAsString(item),
                TTL,
            )
        }
    }

    fun listMarketIndexes(): List<MarketIndexRes> {
        return MarketIndexType.entries.mapNotNull { type ->
            redisTemplate.opsForValue().get("$KEY_PREFIX${type.name}")
                ?.let { objectMapper.readValue(it, MarketIndexRes::class.java) }
        }
    }

    fun getMarketIndex(type: MarketIndexType): MarketIndexRes? {
        return redisTemplate.opsForValue().get("$KEY_PREFIX${type.name}")
            ?.let { objectMapper.readValue(it, MarketIndexRes::class.java) }
    }

    /**
     * 수집이 [STALE_AFTER] 넘게 갱신하지 못한 값이면 마지막 갱신 시각, 아니면 null.
     * TTL 이 1일이라 네이버 실패 시 옛 값이 남는다 — 값을 버리지 않고 브리핑에 "몇 시 기준"으로 표기하는 데 쓴다
     */
    fun staleAt(res: MarketIndexRes): LocalDateTime? =
        res.updatedAt.takeIf { it.isBefore(LocalDateTime.now().minus(STALE_AFTER)) }
}
