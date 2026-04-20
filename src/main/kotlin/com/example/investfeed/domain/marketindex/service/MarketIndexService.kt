package com.example.investfeed.domain.marketindex.service

import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.dto.res.MarketIndexRes
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class MarketIndexService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private const val KEY_PREFIX = "market-index"
        private val TTL = Duration.ofDays(1)
    }

    fun saveAll(list: List<MarketIndexRes>) {
        list.forEach { item ->
            redisTemplate.opsForValue().set(
                "$KEY_PREFIX:${item.type}",
                objectMapper.writeValueAsString(item),
                TTL,
            )
        }
    }

    fun getAll(): List<MarketIndexRes> {
        return MarketIndexType.entries.mapNotNull { type ->
            redisTemplate.opsForValue().get("$KEY_PREFIX:${type.name}")
                ?.let { objectMapper.readValue(it, MarketIndexRes::class.java) }
        }
    }

    fun getByType(type: MarketIndexType): MarketIndexRes? {
        return redisTemplate.opsForValue().get("$KEY_PREFIX:${type.name}")
            ?.let { objectMapper.readValue(it, MarketIndexRes::class.java) }
    }
}
