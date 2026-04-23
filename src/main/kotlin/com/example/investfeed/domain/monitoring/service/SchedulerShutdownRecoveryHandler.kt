package com.example.investfeed.domain.monitoring.service

import com.example.investfeed.domain.monitoring.entity.SchedulerLog
import com.example.investfeed.domain.monitoring.repository.ErrorLogRepository
import com.example.investfeed.domain.monitoring.repository.SchedulerLogRepository
import com.example.investfeed.domain.monitoring.repository.SchedulerStatusRepository
import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.lang.management.ManagementFactory
import java.time.Instant
import java.time.ZoneId

/**
 * 서버 종료 시점에 완료되지 못한 스케줄러 실행을 복구 처리한다.
 *
 * graceful shutdown 과 짝을 이루는 비정상 종료 뒷수습 로직:
 * - graceful shutdown: 정상 종료 시 실행 중인 스케줄러를 최대 30초 대기
 * - ShutdownRecovery: 30초 초과하거나 크래시/SIGKILL 로 강제 종료된 실행을 재기동 시 마감 처리
 *
 * 동작:
 * - 재기동 순간 실행 중이던 스케줄러는 last_started_at > last_finished_at 상태로 남아
 *   다음 정상 실행 전까지 STUCK 으로 잘못 표시됨.
 * - 배포/크래시/kill/STUCK-후-재시작 구분은 DB 만으로 불가능하므로 일률적으로 INTERRUPTED 로 기록.
 * - errorMessage 는 같은 시간대의 error_log 에서 가장 최근 에러를 연결 (예: "LettuceConnectionFactory STOPPED").
 *   상세 trace 는 error_log 탭에서 확인 가능.
 */
@Component
class SchedulerShutdownRecoveryHandler(
    private val schedulerStatusRepository: SchedulerStatusRepository,
    private val schedulerLogRepository: SchedulerLogRepository,
    private val errorLogRepository: ErrorLogRepository,
) {
    private val log = KotlinLogging.logger {}

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun recoverInterruptedRuns() {
        // JVM 기동 시각 — 이 시각 이전에 찍힌 last_started_at 만 "이전 세션 미완료 실행" 으로 간주.
        // 기동 직후 즉시 실행된 스케줄러가 복구 대상으로 잘못 판정되지 않도록.
        val jvmStartedAt = Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().startTime)
            .atZone(ZoneId.systemDefault()).toLocalDateTime()

        val interruptedRuns = schedulerStatusRepository.findAll().filter {
            val started = it.lastStartedAt ?: return@filter false
            // 이번 세션에서 시작된 실행은 제외
            if (!started.isBefore(jvmStartedAt)) return@filter false
            it.lastFinishedAt == null || it.lastFinishedAt!!.isBefore(started)
        }

        if (interruptedRuns.isEmpty()) return

        interruptedRuns.forEach { s ->
            val started = s.lastStartedAt!!
            // managed entity 라 dirty checking 으로 자동 UPDATE (save 불필요)
            s.lastFinishedAt = started

            // MDC 로 매칭된 해당 스케줄러의 에러 중 시간 범위 내 가장 최근 것을 연결.
            // MDC 없이 발생한 shutdown cascade 에러(Netty/Lettuce 등)는 매칭 안 됨 → errorMessage null.
            val relatedError = runCatching {
                errorLogRepository.findTopBySchedulerNameAndOccurredAtBetweenOrderByOccurredAtDesc(
                    s.schedulerName, started, jvmStartedAt,
                )
            }.getOrNull()

            runCatching {
                schedulerLogRepository.save(
                    SchedulerLog(
                        schedulerName = s.schedulerName,
                        startedAt = started,
                        finishedAt = started,
                        durationMs = 0,
                        status = "INTERRUPTED",
                        errorMessage = relatedError?.message,
                    )
                )
            }.onFailure { log.error { "[shutdown-recovery] scheduler_log INSERT 실패 (${s.schedulerName}): ${it.message}" } }

            log.info { "[shutdown-recovery] ${s.schedulerName} INTERRUPTED 기록. 연결된 error_log: ${relatedError?.id}" }
        }
    }
}
