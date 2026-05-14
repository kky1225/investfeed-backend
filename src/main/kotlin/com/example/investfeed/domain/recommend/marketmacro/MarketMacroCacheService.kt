package com.example.investfeed.domain.recommend.marketmacro

import com.example.investfeed.global.constant.RedisKeyPrefix
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * 코스피/코스닥 매크로 시그널 Redis 캐시 관리.
 *
 * - 1분 단위 [MarketMacroScheduler] 가 [saveSnapshot] 호출하여 캐시 갱신
 * - 사용자 추천 조회 시점에 [MarketIndexAdjustmentModule] 이 [getSnapshot] 호출
 * - TTL 1일 — 장 종료 후 캐시 보존, 다음 거래일 09:00 polling 까지 유지
 *
 * Key format: `market-macro:KOSPI`, `market-macro:KOSDAQ`
 */
@Service
class MarketMacroCacheService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    private val KEY_PREFIX = RedisKeyPrefix.MARKET_MACRO.prefix

    companion object {
        private val TTL: Duration = Duration.ofDays(1)
    }

    fun saveSnapshot(snapshot: MarketMacroSnapshot) {
        redisTemplate.opsForValue().set(
            "$KEY_PREFIX${snapshot.marketType}",
            objectMapper.writeValueAsString(snapshot),
            TTL,
        )
    }

    fun getSnapshot(marketType: String): MarketMacroSnapshot? {
        return redisTemplate.opsForValue().get("$KEY_PREFIX$marketType")
            ?.let { objectMapper.readValue(it, MarketMacroSnapshot::class.java) }
    }
}
