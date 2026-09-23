package com.example.investfeed.domain.assistant.service

import com.example.investfeed.global.constant.RedisKeyPrefix
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate

@Service
class AlertStateService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val TTL: Duration = Duration.ofHours(26)
    }

    data class IndexAlertState(
        var firedUpLevels: MutableList<Double> = mutableListOf(),    // 발동한 상승 임계
        var firedDownLevels: MutableList<Double> = mutableListOf(),  // 발동한 하락 임계
        var cbStages: MutableList<Int> = mutableListOf(),
        var cbPendingStage: Int? = null,
    )

    fun get(indexCode: String, date: LocalDate): IndexAlertState =
        redisTemplate.opsForValue().get(key(indexCode, date))
            ?.let { runCatching { objectMapper.readValue(it, IndexAlertState::class.java) }
                .onFailure { e -> log.error(e) { "알림 상태 역직렬화 실패: $indexCode $date" } }.getOrNull() }
            ?: IndexAlertState()

    fun save(indexCode: String, date: LocalDate, state: IndexAlertState) {
        redisTemplate.opsForValue().set(key(indexCode, date), objectMapper.writeValueAsString(state), TTL)
    }

    private fun key(indexCode: String, date: LocalDate) = "${RedisKeyPrefix.ASSISTANT.prefix}ALERT:$indexCode:$date"
}
