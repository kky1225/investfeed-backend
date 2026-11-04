package com.example.investfeed.domain.monitoring.service

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.global.constant.RedisKeyPrefix
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.LocalDate

/** 제공자별 하루치 호출 통계. 화면 표시와 DB 적재에 함께 쓴다 */
data class ApiCallDailySnapshot(
    val provider: ApiProvider,
    val date: LocalDate,
    val callCount: Long,
    val failed: Long,
    val retried: Long,
    val latencyCount: Long,
    val latencyTotalMs: Long,
    val latencyMaxMs: Long,
) {
    val isEmpty: Boolean
        get() = callCount == 0L && failed == 0L && retried == 0L && latencyCount == 0L

    val avgLatencyMs: Long?
        get() = if (latencyCount > 0) latencyTotalMs / latencyCount else null

    val maxLatencyMs: Long?
        get() = if (latencyCount > 0) latencyMaxMs else null

    companion object {
        internal fun of(
            provider: ApiProvider,
            date: LocalDate,
            callCount: Long = 0,
            failed: Long = 0,
            retried: Long = 0,
            latencyCount: Long = 0,
            latencyTotalMs: Long = 0,
            latencyMaxMs: Long = 0,
        ) = ApiCallDailySnapshot(
            provider = provider,
            date = date,
            callCount = callCount,
            failed = failed.coerceAtLeast(0),
            retried = retried,
            latencyCount = latencyCount,
            latencyTotalMs = latencyTotalMs,
            latencyMaxMs = latencyMaxMs.coerceAtLeast(0),
        )
    }
}

/**
 * 외부 API 일별 호출 카운터.
 *
 * - 키 형식: `api_call:{provider}:{yyyy-MM-dd}` (예: `api_call:KIWOOM:2026-04-26`)
 * - 매 호출마다 INCR (atomic). 첫 INCR 시 8일 TTL 설정 — 7일 조회 + 여유 1일.
 * - Redis 장애 시 외부 API 호출이 차단되면 안 되므로 모든 연산은 try-catch 로 감싸 실패 무시.
 * - 실패·재시도·응답 시간 키 형식: `api_call_health:{provider}:{yyyy-MM-dd}:{항목}`
 * - 실패·재시도·응답 시간은 응답 처리 스레드(netty)에서 호출되므로 Redis 쓰기를 별도 스레드로 넘긴다.
 */
@Service
class ApiCallCounterService(
    private val redisTemplate: StringRedisTemplate,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val TTL: Duration = Duration.ofDays(8)
        private const val FAILED = "failed"
        private const val RETRIED = "retried"
        private const val LATENCY_COUNT = "latency_count"
        private const val LATENCY_TOTAL_MS = "latency_total_ms"
        private const val LATENCY_MAX_MS = "latency_max_ms"
        private val HEALTH_FIELDS = listOf(FAILED, RETRIED, LATENCY_COUNT, LATENCY_TOTAL_MS, LATENCY_MAX_MS)
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

    /** 화면 조회용. 6일 전부터 오늘까지 오래된 날짜 순. Redis 오류 시 모든 값을 0 으로 돌려준다 */
    fun getRecent7Days(provider: ApiProvider): List<ApiCallDailySnapshot> {
        val today = LocalDate.now()
        val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        return runCatching { getDailySnapshots(provider, days) }
            .getOrElse { days.map { ApiCallDailySnapshot.of(provider, it) } }
    }

    fun recordResult(provider: ApiProvider, failed: Boolean, latencyMs: Long) = writeAsync(provider) {
        val today = LocalDate.now()
        if (failed) addHealth(provider, today, FAILED, 1)
        addHealth(provider, today, LATENCY_COUNT, 1)
        addHealth(provider, today, LATENCY_TOTAL_MS, latencyMs)

        val maxKey = healthKey(provider, today, LATENCY_MAX_MS)
        val currentMax = redisTemplate.opsForValue().get(maxKey)?.toLongOrNull() ?: -1L
        if (latencyMs > currentMax) redisTemplate.opsForValue().set(maxKey, latencyMs.toString(), TTL)
    }

    /** failureRecorded: 재시도 전 응답이 이미 실패로 집계된 경우 true. 최종 결과가 아니므로 실패에서 뺀다 */
    fun recordRetry(provider: ApiProvider, failureRecorded: Boolean = false) = writeAsync(provider) {
        val today = LocalDate.now()
        addHealth(provider, today, RETRIED, 1)
        if (failureRecorded) addHealth(provider, today, FAILED, -1)
    }

    /** DB 적재용. 화면 조회와 달리 Redis 오류를 삼키지 않는다 (빈 값을 정상으로 오인하지 않도록) */
    fun getDailySnapshots(provider: ApiProvider, dates: List<LocalDate>): List<ApiCallDailySnapshot> {
        val keys = dates.flatMap { date -> listOf(keyOf(provider, date)) + HEALTH_FIELDS.map { healthKey(provider, date, it) } }
        val values = redisTemplate.opsForValue().multiGet(keys)
            ?: throw IllegalStateException("Redis 조회 결과 없음: ${provider.name}")
        return dates.zip(values.chunked(HEALTH_FIELDS.size + 1)) { date, row ->
            val numbers = row.map { it?.toLongOrNull() ?: 0L }
            ApiCallDailySnapshot.of(
                provider = provider,
                date = date,
                callCount = numbers[0],
                failed = numbers[1],
                retried = numbers[2],
                latencyCount = numbers[3],
                latencyTotalMs = numbers[4],
                latencyMaxMs = numbers[5],
            )
        }
    }

    private fun writeAsync(provider: ApiProvider, write: () -> Unit) {
        try {
            Schedulers.boundedElastic().schedule {
                try {
                    write()
                } catch (e: Exception) {
                    log.debug { "API health counter 실패 (provider=${provider.name}): ${e.message}" }
                }
            }
        } catch (e: Exception) {
            log.debug { "API health counter 예약 실패 (provider=${provider.name}): ${e.message}" }
        }
    }

    private fun addHealth(provider: ApiProvider, date: LocalDate, field: String, delta: Long) {
        val key = healthKey(provider, date, field)
        val newVal = redisTemplate.opsForValue().increment(key, delta) ?: return
        if (newVal == delta) redisTemplate.expire(key, TTL)
    }

    private fun healthKey(provider: ApiProvider, date: LocalDate, field: String): String =
        "${RedisKeyPrefix.API_CALL_HEALTH.prefix}${provider.name}:$date:$field"

    private fun todayKey(provider: ApiProvider) = keyOf(provider, LocalDate.now())

    private fun keyOf(provider: ApiProvider, date: LocalDate): String =
        "${RedisKeyPrefix.API_CALL.prefix}${provider.name}:$date"
}
