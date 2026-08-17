package com.example.investfeed.domain.monitoring.scheduler

import com.example.investfeed.domain.monitoring.repository.ErrorLogRepository
import com.example.investfeed.domain.monitoring.repository.LogAckHistoryRepository
import com.example.investfeed.domain.monitoring.repository.SchedulerLogRepository
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class SchedulerLogCleanupScheduler(
    private val schedulerLogRepository: SchedulerLogRepository,
    private val errorLogRepository: ErrorLogRepository,
    private val logAckHistoryRepository: LogAckHistoryRepository,
    private val schedulerLogService: SchedulerLogService,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = SchedulerCron.SCHEDULER_LOG_CLEANUP, scheduler = "slowScheduler")
    @Transactional
    fun cleanup() {
        schedulerLogService.execute(SchedulerName.SchedulerLogCleanupScheduler) {
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
