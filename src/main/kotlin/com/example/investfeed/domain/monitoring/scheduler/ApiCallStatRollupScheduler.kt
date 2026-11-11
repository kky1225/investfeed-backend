package com.example.investfeed.domain.monitoring.scheduler

import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.ApiCallStatRollupService
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ApiCallStatRollupScheduler(
    private val apiCallStatRollupService: ApiCallStatRollupService,
    private val schedulerLogService: SchedulerLogService,
) {

    @Scheduled(cron = SchedulerCron.API_CALL_STAT_ROLLUP, scheduler = "slowScheduler")
    fun rollup() {
        schedulerLogService.execute(SchedulerName.ApiCallStatRollupScheduler) {
            report("적재 ${apiCallStatRollupService.rollup()}건")
        }
    }
}
