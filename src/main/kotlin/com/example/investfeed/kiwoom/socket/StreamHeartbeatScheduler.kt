package com.example.investfeed.kiwoom.socket

import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.kiwoom.config.WebSocketHandler
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class StreamHeartbeatScheduler(
    private val webSocketHandler: WebSocketHandler,
    private val schedulerLogService: SchedulerLogService,
) {
    @Scheduled(cron = SchedulerCron.STREAM_HEARTBEAT, scheduler = "fastScheduler")
    fun sendHeartbeat() {
        schedulerLogService.execute(SchedulerName.StreamHeartbeatScheduler) {
            webSocketHandler.sendHeartbeat()
        }
    }
}
