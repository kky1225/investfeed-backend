package com.example.investfeed.domain.monitoring.service

import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.logging.ApplicationShutdownState
import com.example.investfeed.domain.monitoring.repository.SchedulerStatusRepository
import com.example.investfeed.common.exception.AlreadyLogged
import com.example.investfeed.common.exception.InvestFeedException
import com.example.investfeed.common.util.failure
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

enum class SchedulerType { FAST, SLOW }

/** SchedulerLogService 가 실패 기록을 마친 뒤 던지는 예외. 원래 예외는 cause */
class SchedulerExecutionException(schedulerName: String, cause: Throwable) :
    InvestFeedException(code = "SCHEDULER_9999", message = "$schedulerName 실행 실패: ${cause.message}", cause = cause), AlreadyLogged

/**
 * 스케줄러 본문이 성공 요약을 넘기는 통로. 본문에서 report("저장 12건") 처럼 부른다.
 * 성공 로그는 본문·서비스에서 남기지 않고 execute 가 끝날 때 이 요약을 붙여 1건만 남긴다.
 */
class SchedulerRun internal constructor() {
    private val parts = mutableListOf<String>()

    fun report(summary: String) {
        parts += summary
    }

    internal fun summary(): String? = parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
}

@Service
class SchedulerLogService(
    private val schedulerStatusRepository: SchedulerStatusRepository,
    private val recordWriter: SchedulerRecordWriter,
    private val failureAlertService: SchedulerFailureAlertService,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        const val MDC_SCHEDULER_NAME = "schedulerName"
    }

    /**
     * 스케줄러 1회 실행. 성공·실패 로그는 모두 여기서 1건만 남긴다.
     * - 성공: SLOW 는 info, FAST(매분·매초)는 요약(report)이 있을 때만 debug — "[이름] 완료 120ms — 요약"
     * - timeout 초과: warn / 실패: failure (error 또는 이미 기록된 실패면 warn)
     */
    fun <T> execute(scheduler: SchedulerName, block: SchedulerRun.() -> T): T {
        val name = scheduler.name
        val type = scheduler.type
        val startedAt = LocalDateTime.now()
        val run = SchedulerRun()
        MDC.put(MDC_SCHEDULER_NAME, name)
        markFired(scheduler)
        runCatching { recordWriter.markStarted(scheduler, startedAt) }
            .onFailure { log.error { "scheduler_status markStarted 실패 ($name): ${it.message}" } }

        return try {
            val result = run.block()
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

            val summary = run.summary()
            val message = "[$name] 완료 ${durationMs}ms" + (summary?.let { " — $it" } ?: "")
            when {
                timeoutExceeded -> log.warn { "$message (timeout ${timeoutSec}s 초과)" }
                type == SchedulerType.SLOW -> log.info { message }
                // FAST(매초·5초·매분)는 요약이 있을 때만 남긴다. 할 일이 없던 실행은 scheduler_status 만 갱신
                summary != null -> log.debug { message }
            }

            result
        } catch (e: Exception) {
            if (ApplicationShutdownState.isShuttingDown) {
                throw e
            }

            val finishedAt = LocalDateTime.now()
            val durationMs = Duration.between(startedAt, finishedAt).toMillis()
            // 스케줄러 실패는 여기서 1건만 남긴다. 이미 기록된 외부 API 실패면 warn
            log.failure(e) { "$name 실행 실패" }

            val message = listOfNotNull(e.message, e.cause?.message?.takeIf { it != e.message })
                .joinToString(" / ").take(2000).ifEmpty { null }

            runCatching { recordWriter.upsertFailure(scheduler, finishedAt, message) }
                .onFailure { log.error { "scheduler_status 실패 UPSERT 실패 ($name): ${it.message}" } }

            runCatching {
                recordWriter.appendLog(scheduler, startedAt, finishedAt, durationMs, "FAILED", message)
            }.onFailure { log.error { "scheduler_log 실패 INSERT 실패 ($name): ${it.message}" } }

            runCatching { failureAlertService.notifyFailure(scheduler, message) }
                .onFailure { log.error { "스케줄러 실패 알림 오류 ($name): ${it.message}" } }

            // 기록을 마쳤다는 표시로 감싸서 던진다 → Spring 스케줄러 오류 처리기가 다시 error 로 남기지 않는다
            throw SchedulerExecutionException(name, e)
        } finally {
            MDC.remove(MDC_SCHEDULER_NAME)
        }
    }

    /** 실행 조건이 안 돼 execute 를 부르지 않고 끝낼 때 (휴일·세션 밖 등). 성공이 아니므로 debug */
    fun skip(scheduler: SchedulerName, reason: String) {
        log.debug { "[${scheduler.name}] 건너뜀 — $reason" }
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
