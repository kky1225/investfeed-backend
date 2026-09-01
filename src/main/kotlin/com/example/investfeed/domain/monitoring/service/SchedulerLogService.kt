package com.example.investfeed.domain.monitoring.service

import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.logging.ApplicationShutdownState
import com.example.investfeed.domain.monitoring.repository.SchedulerStatusRepository
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

enum class SchedulerType { FAST, SLOW }

@Service
class SchedulerLogService(
    private val schedulerStatusRepository: SchedulerStatusRepository,
    private val recordWriter: SchedulerRecordWriter,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        const val MDC_SCHEDULER_NAME = "schedulerName"
    }

    fun <T> execute(scheduler: SchedulerName, block: () -> T): T {
        val name = scheduler.name
        val type = scheduler.type
        val startedAt = LocalDateTime.now()
        MDC.put(MDC_SCHEDULER_NAME, name)
        markFired(scheduler)
        runCatching { recordWriter.markStarted(scheduler, startedAt) }
            .onFailure { log.error { "scheduler_status markStarted 실패 ($name): ${it.message}" } }

        return try {
            val result = block()
            val finishedAt = LocalDateTime.now()
            val durationMs = Duration.between(startedAt, finishedAt).toMillis()

            val timeoutSec = runCatching {
                schedulerStatusRepository.findById(name).orElse(null)?.timeoutSec ?: 180
            }.getOrDefault(180)
            val timeoutExceeded = durationMs > timeoutSec * 1000L

            runCatching { recordWriter.upsertSuccess(scheduler, finishedAt, durationMs) }
                .onFailure { log.error { "scheduler_status 성공 UPSERT 실패 ($name): ${it.message}" } }

            // SLOW 는 항상 기록 / FAST 는 timeout 초과한 경우만 기록
            if (type == SchedulerType.SLOW || timeoutExceeded) {
                val logStatus = if (timeoutExceeded) "INTERRUPTED" else "SUCCESS"
                val message = if (timeoutExceeded) "실행 시간이 timeout(${timeoutSec}s)을 초과했습니다" else null
                runCatching {
                    recordWriter.appendLog(scheduler, startedAt, finishedAt, durationMs, logStatus, message)
                }.onFailure { log.error { "scheduler_log INSERT 실패 ($name): ${it.message}" } }
            }

            result
        } catch (e: Exception) {
            if (ApplicationShutdownState.isShuttingDown) {
                throw e
            }

            val finishedAt = LocalDateTime.now()
            val durationMs = Duration.between(startedAt, finishedAt).toMillis()
            val message = e.message?.take(2000)

            runCatching { recordWriter.upsertFailure(scheduler, finishedAt, message) }
                .onFailure { log.error { "scheduler_status 실패 UPSERT 실패 ($name): ${it.message}" } }

            runCatching {
                recordWriter.appendLog(scheduler, startedAt, finishedAt, durationMs, "FAILED", message)
            }.onFailure { log.error { "scheduler_log 실패 INSERT 실패 ($name): ${it.message}" } }

            throw e
        } finally {
            MDC.remove(MDC_SCHEDULER_NAME)
        }
    }

    fun markFired(scheduler: SchedulerName) {
        runCatching { recordWriter.updateFiredAt(scheduler, LocalDateTime.now()) }
            .onFailure { log.error { "scheduler_status last_fired_at 갱신 실패 (${scheduler.name}): ${it.message}" } }
    }

    fun isRunning(scheduler: SchedulerName): Boolean {
        val status = schedulerStatusRepository.findById(scheduler.name).orElse(null) ?: return false
        val started = status.lastStartedAt ?: return false
        val finished = status.lastFinishedAt
        if (finished != null && !finished.isBefore(started)) return false
        val elapsed = Duration.between(started, LocalDateTime.now()).seconds
        return elapsed <= status.timeoutSec
    }

    fun isSucceededSince(scheduler: SchedulerName, since: LocalDateTime): Boolean {
        val status = schedulerStatusRepository.findById(scheduler.name).orElse(null) ?: return false
        val lastSuccess = status.lastSuccessAt ?: return false
        return !lastSuccess.isBefore(since)
    }

    fun lastSuccessAt(scheduler: SchedulerName): LocalDateTime? =
        schedulerStatusRepository.findById(scheduler.name).orElse(null)?.lastSuccessAt

}
