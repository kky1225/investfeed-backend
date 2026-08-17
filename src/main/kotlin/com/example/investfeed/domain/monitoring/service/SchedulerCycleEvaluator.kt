package com.example.investfeed.domain.monitoring.service

import com.example.investfeed.domain.monitoring.enum.SchedulerFireStatus
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import mu.KotlinLogging
import org.springframework.scheduling.support.CronExpression
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

@Component
class SchedulerCycleEvaluator {
    private val log = KotlinLogging.logger {}

    companion object {
        val CYCLE_START_TIME: LocalTime = LocalTime.of(22, 0)
        private val MAX_GRACE: Duration = Duration.ofMinutes(10)
        private val MIN_GRACE: Duration = Duration.ofMinutes(1)
        private val FIRE_SKEW: Duration = Duration.ofSeconds(30)
        private val MIN_TRACKED_INTERVAL: Duration = Duration.ofDays(1)
    }

    fun cycleStart(now: LocalDateTime): LocalDateTime {
        val todayCycle = now.toLocalDate().atTime(CYCLE_START_TIME)
        return if (now.isBefore(todayCycle)) todayCycle.minusDays(1) else todayCycle
    }

    fun evaluate(
        scheduler: SchedulerName,
        lastFiredAt: LocalDateTime?,
        lastStartedAt: LocalDateTime?,
        now: LocalDateTime,
        isHoliday: Boolean,
    ): SchedulerFireStatus {
        if (scheduler.blockedOnHoliday && isHoliday) return SchedulerFireStatus.NONE

        val expressions = scheduler.crons.mapNotNull { expr ->
            runCatching { CronExpression.parse(expr) }
                .onFailure { log.error { "cron 파싱 실패 (${scheduler.name}): $expr — ${it.message}" } }
                .getOrNull()
        }
        if (expressions.isEmpty()) return SchedulerFireStatus.NONE

        val cycleStart = cycleStart(now)

        val interval = intervalOf(expressions, cycleStart) ?: return SchedulerFireStatus.NONE
        if (interval < MIN_TRACKED_INTERVAL) return SchedulerFireStatus.NONE

        val firedAt = listOfNotNull(lastFiredAt, lastStartedAt).maxOrNull()
        val firedInCycle = firedAt != null && !firedAt.isBefore(cycleStart)

        val base = if (firedInCycle) firedAt!!.plus(FIRE_SKEW) else cycleStart.minusNanos(1)
        val nextDue = nextAfter(expressions, base) ?: return if (firedInCycle) SchedulerFireStatus.FIRED else SchedulerFireStatus.NONE

        if (nextDue.plus(graceOf(expressions, nextDue)).isBefore(now)) return SchedulerFireStatus.MISSED

        return if (firedInCycle) SchedulerFireStatus.FIRED else SchedulerFireStatus.NONE
    }

    private fun nextAfter(expressions: List<CronExpression>, from: LocalDateTime): LocalDateTime? =
        expressions.mapNotNull { it.next(from) }.minOrNull()

    private fun graceOf(expressions: List<CronExpression>, first: LocalDateTime): Duration {
        val interval = intervalOf(expressions, first.minusNanos(1)) ?: return MAX_GRACE
        return interval.multipliedBy(2)
            .coerceAtMost(MAX_GRACE)
            .coerceAtLeast(MIN_GRACE)
    }

    private fun intervalOf(expressions: List<CronExpression>, from: LocalDateTime): Duration? {
        val first = nextAfter(expressions, from) ?: return null
        val second = nextAfter(expressions, first) ?: return null
        return Duration.between(first, second)
    }
}
