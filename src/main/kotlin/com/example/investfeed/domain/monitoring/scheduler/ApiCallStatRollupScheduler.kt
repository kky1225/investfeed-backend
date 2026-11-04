package com.example.investfeed.domain.monitoring.scheduler

import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.ApiCallStatRollupService
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ApiCallStatRollupScheduler(
    private val apiCallStatRollupService: ApiCallStatRollupService,
    private val schedulerLogService: SchedulerLogService,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = SchedulerCron.API_CALL_STAT_ROLLUP, scheduler = "slowScheduler")
    fun rollup() {
        schedulerLogService.execute(SchedulerName.ApiCallStatRollupScheduler) {
            val saved = apiCallStatRollupService.rollup()
            log.info { "외부 API 일별 통계 적재 완료: ${saved}건" }
        }
    }
}
