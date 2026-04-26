package com.example.investfeed.domain.monitoring.service

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.global.constant.RedisKeyPrefix
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate

/**
 * 외부 API 일별 호출 카운터.
 *
 * - 키 형식: `api_call:{provider}:{yyyy-MM-dd}` (예: `api_call:KIWOOM:2026-04-26`)
 * - 매 호출마다 INCR (atomic). 첫 INCR 시 8일 TTL 설정 — 7일 조회 + 여유 1일.
 * - Redis 장애 시 외부 API 호출이 차단되면 안 되므로 모든 연산은 try-catch 로 감싸 실패 무시.
 */
@Service
class ApiCallCounterService(
    private val redisTemplate: StringRedisTemplate,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val TTL: Duration = Duration.ofDays(8)
    }

    fun increment(provider: ApiProvider) {
        val key = todayKey(provider)
        try {
            val newVal = redisTemplate.opsForValue().increment(key) ?: 0L
            if (newVal == 1L) {
                redisTemplate.expire(key, TTL)
            }
        } catch (e: Exception) {
            log.debug { "API call counter 실패 (provider=${provider.name}): ${e.message}" }
        }
    }

    fun getRecent7Days(provider: ApiProvider): List<Pair<LocalDate, Long>> {
        val today = LocalDate.now()
        val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val keys = days.map { keyOf(provider, it) }
        val values = runCatching { redisTemplate.opsForValue().multiGet(keys) ?: emptyList() }
            .getOrDefault(emptyList())
        return days.mapIndexed { idx, date ->
            val v = values.getOrNull(idx)?.toLongOrNull() ?: 0L
            date to v
        }
    }

    private fun todayKey(provider: ApiProvider) = keyOf(provider, LocalDate.now())

    private fun keyOf(provider: ApiProvider, date: LocalDate): String =
        "${RedisKeyPrefix.API_CALL.prefix}${provider.name}:$date"
}
