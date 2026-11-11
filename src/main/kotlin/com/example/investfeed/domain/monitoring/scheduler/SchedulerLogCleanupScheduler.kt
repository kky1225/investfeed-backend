package com.example.investfeed.domain.monitoring.scheduler

import com.example.investfeed.domain.monitoring.repository.ErrorLogRepository
import com.example.investfeed.domain.monitoring.repository.LogAckHistoryRepository
import com.example.investfeed.domain.monitoring.repository.SchedulerLogRepository
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
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

    @Scheduled(cron = SchedulerCron.SCHEDULER_LOG_CLEANUP, scheduler = "slowScheduler")
    @Transactional
    fun cleanup() {
        schedulerLogService.execute(SchedulerName.SchedulerLogCleanupScheduler) {
            val threshold = LocalDateTime.now().minusDays(90)

            val deletedSchedulerLogs = schedulerLogRepository.deleteByStartedAtBefore(threshold)
            val deletedErrorLogs = errorLogRepository.deleteByOccurredAtBefore(threshold)
            val deletedAckHistory = logAckHistoryRepository.deleteByActedAtBefore(threshold)
            report(
                "기준 $threshold 이전 삭제 — scheduler_log ${deletedSchedulerLogs}건, " +
                    "error_log ${deletedErrorLogs}건, log_ack_history ${deletedAckHistory}건"
            )
        }
    }
}
