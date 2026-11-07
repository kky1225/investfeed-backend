package com.example.investfeed.toss.socket

import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TossSocketCleanupScheduler(
    private val tossStreamClient: TossStreamClient,
    private val schedulerLogService: SchedulerLogService,
) {
    @Scheduled(cron = SchedulerCron.TOSS_SOCKET_CLEANUP, scheduler = "fastScheduler")
    fun releaseIdleConnections() {
        schedulerLogService.execute(SchedulerName.TossSocketCleanupScheduler) {
            tossStreamClient.releaseIdleConnections()
        }
    }
}
