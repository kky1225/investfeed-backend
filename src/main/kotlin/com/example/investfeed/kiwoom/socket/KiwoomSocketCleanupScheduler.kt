package com.example.investfeed.kiwoom.socket

import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class KiwoomSocketCleanupScheduler(
    private val socketManager: KiwoomSocketManager,
    private val schedulerLogService: SchedulerLogService,
) {
    @Scheduled(cron = SchedulerCron.KIWOOM_SOCKET_CLEANUP, scheduler = "fastScheduler")
    fun releaseIdleConnections() {
        schedulerLogService.execute(SchedulerName.KiwoomSocketCleanupScheduler) {
            socketManager.releaseIdleConnections()
        }
    }
}
