package com.example.investfeed.domain.monitoring.service

import com.example.investfeed.domain.monitoring.entity.SchedulerLog
import com.example.investfeed.domain.monitoring.entity.SchedulerStatus
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.logging.ApplicationShutdownState
import com.example.investfeed.domain.monitoring.repository.SchedulerLogRepository
import com.example.investfeed.domain.monitoring.repository.SchedulerStatusRepository
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

enum class SchedulerType { FAST, SLOW }

/**
 * 스케줄러 실행을 감싸서 실행 이력/최근 상태를 기록한다.
 *
 * - FAST (매분): 정상 실행 시 scheduler_status 만 UPSERT, scheduler_log INSERT 스킵.
 *                단, duration > timeout_sec 이면 INTERRUPTED 로 scheduler_log INSERT.
 * - SLOW (시간/일 단위): 성공/실패 무관 scheduler_log INSERT + scheduler_status UPSERT.
 *   · duration > timeout_sec 이어도 결국 성공한 경우 status='INTERRUPTED' 로 기록 → 24h WARNING 표시.
 *
 * 기록 실패가 실제 스케줄러 동작을 막지 않도록 모든 DB 호출은 runCatching 으로 감싼다.
 */
@Service
class SchedulerLogService(
    private val schedulerLogRepository: SchedulerLogRepository,
    private val schedulerStatusRepository: SchedulerStatusRepository,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        /** MDC 키. 스케줄러 실행 중 발생한 ERROR 로그를 어떤 스케줄러 것인지 식별하기 위함. */
        const val MDC_SCHEDULER_NAME = "schedulerName"
    }

    fun <T> execute(scheduler: SchedulerName, block: () -> T): T {
        val name = scheduler.name
        val type = scheduler.type
        val startedAt = LocalDateTime.now()
        MDC.put(MDC_SCHEDULER_NAME, name)
        runCatching { markStarted(scheduler, startedAt) }
            .onFailure { log.error { "scheduler_status markStarted 실패 ($name): ${it.message}" } }

        return try {
            val result = block()
            val finishedAt = LocalDateTime.now()
            val durationMs = Duration.between(startedAt, finishedAt).toMillis()

            val timeoutSec = runCatching {
                schedulerStatusRepository.findById(name).orElse(null)?.timeoutSec ?: 180
            }.getOrDefault(180)
            val timeoutExceeded = durationMs > timeoutSec * 1000L

            runCatching { upsertSuccess(scheduler, finishedAt, durationMs) }
                .onFailure { log.error { "scheduler_status 성공 UPSERT 실패 ($name): ${it.message}" } }

            // SLOW 는 항상 기록 / FAST 는 timeout 초과한 경우만 기록
            if (type == SchedulerType.SLOW || timeoutExceeded) {
                val logStatus = if (timeoutExceeded) "INTERRUPTED" else "SUCCESS"
                val message = if (timeoutExceeded) "실행 시간이 timeout(${timeoutSec}s)을 초과했습니다" else null
                runCatching {
                    schedulerLogRepository.save(
                        SchedulerLog(
                            schedulerName = name,
                            startedAt = startedAt,
                            finishedAt = finishedAt,
                            durationMs = durationMs,
                            status = logStatus,
                            errorMessage = message,
                        )
                    )
                }.onFailure { log.error { "scheduler_log INSERT 실패 ($name): ${it.message}" } }
            }

            result
        } catch (e: Exception) {
            // shutdown 중 race condition 에러는 DB 기록 skip
            // → 재기동 시 orphan cleaner 가 INTERRUPTED 로 정리
            if (ApplicationShutdownState.isShuttingDown) {
                throw e
            }

            val finishedAt = LocalDateTime.now()
            val durationMs = Duration.between(startedAt, finishedAt).toMillis()
            val message = e.message?.take(2000)

            runCatching { upsertFailure(scheduler, finishedAt, message) }
                .onFailure { log.error { "scheduler_status 실패 UPSERT 실패 ($name): ${it.message}" } }

            runCatching {
                schedulerLogRepository.save(
                    SchedulerLog(
                        schedulerName = name,
                        startedAt = startedAt,
                        finishedAt = finishedAt,
                        durationMs = durationMs,
                        status = "FAILED",
                        errorMessage = message,
                    )
                )
            }.onFailure { log.error { "scheduler_log 실패 INSERT 실패 ($name): ${it.message}" } }

            throw e
        } finally {
            MDC.remove(MDC_SCHEDULER_NAME)
        }
    }

    /**
     * 해당 스케줄러가 현재 실행 중인지 판정.
     * - lastStartedAt 이 lastFinishedAt 보다 이후이고 timeout 내라면 실행 중으로 본다.
     * - row 자체가 없으면 false.
     * cron 핸들러 간 상호배제 가드 및 ManualTriggerService 에서 사용.
     */
    fun isRunning(scheduler: SchedulerName): Boolean {
        val status = schedulerStatusRepository.findById(scheduler.name).orElse(null) ?: return false
        val started = status.lastStartedAt ?: return false
        val finished = status.lastFinishedAt
        if (finished != null && !finished.isBefore(started)) return false
        val elapsed = Duration.between(started, LocalDateTime.now()).seconds
        return elapsed <= status.timeoutSec
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    internal fun markStarted(scheduler: SchedulerName, at: LocalDateTime) {
        val entity = schedulerStatusRepository.findById(scheduler.name).orElse(
            SchedulerStatus(schedulerName = scheduler.name, schedulerType = scheduler.type.name)
        )
        entity.schedulerType = scheduler.type.name
        entity.lastStartedAt = at
        entity.updatedAt = LocalDateTime.now()
        schedulerStatusRepository.save(entity)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    internal fun upsertSuccess(scheduler: SchedulerName, finishedAt: LocalDateTime, durationMs: Long) {
        val entity = schedulerStatusRepository.findById(scheduler.name).orElse(
            SchedulerStatus(schedulerName = scheduler.name, schedulerType = scheduler.type.name)
        )
        entity.schedulerType = scheduler.type.name
        entity.lastFinishedAt = finishedAt
        entity.lastSuccessAt = finishedAt
        entity.lastSuccessDurationMs = durationMs
        entity.updatedAt = LocalDateTime.now()
        schedulerStatusRepository.save(entity)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    internal fun upsertFailure(scheduler: SchedulerName, finishedAt: LocalDateTime, message: String?) {
        val entity = schedulerStatusRepository.findById(scheduler.name).orElse(
            SchedulerStatus(schedulerName = scheduler.name, schedulerType = scheduler.type.name)
        )
        entity.schedulerType = scheduler.type.name
        entity.lastFinishedAt = finishedAt
        entity.lastFailureAt = finishedAt
        entity.lastFailureMessage = message
        entity.updatedAt = LocalDateTime.now()
        schedulerStatusRepository.save(entity)
    }
}
