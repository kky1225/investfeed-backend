package com.example.investfeed.domain.monitoring.scheduler

import com.example.investfeed.domain.monitoring.repository.ErrorLogRepository
import com.example.investfeed.domain.monitoring.repository.LogAckHistoryRepository
import com.example.investfeed.domain.monitoring.repository.SchedulerLogRepository
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.monitoring.service.SchedulerType
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * scheduler_log 90일 초과 행 자동 정리.
 * 매일 새벽 4시 실행.
 */
@Component
class SchedulerLogCleanupScheduler(
    private val schedulerLogRepository: SchedulerLogRepository,
    private val errorLogRepository: ErrorLogRepository,
    private val logAckHistoryRepository: LogAckHistoryRepository,
    private val schedulerLogService: SchedulerLogService,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 4 * * *", scheduler = "slowScheduler")
    @Transactional
    fun cleanup() {
        schedulerLogService.execute("SchedulerLogCleanupScheduler", SchedulerType.SLOW) {
            val threshold = LocalDateTime.now().minusDays(90)

            val deletedSchedulerLogs = schedulerLogRepository.deleteByStartedAtBefore(threshold)
            log.info { "scheduler_log 정리 완료: ${deletedSchedulerLogs}건 삭제 (기준: $threshold)" }

            val deletedErrorLogs = errorLogRepository.deleteByOccurredAtBefore(threshold)
            log.info { "error_log 정리 완료: ${deletedErrorLogs}건 삭제 (기준: $threshold)" }

            val deletedAckHistory = logAckHistoryRepository.deleteByActedAtBefore(threshold)
            log.info { "log_ack_history 정리 완료: ${deletedAckHistory}건 삭제 (기준: $threshold)" }
        }
    }
}
