package com.example.investfeed.domain.papertrade.scheduler

import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.papertrade.service.HoldingGradeService
import com.example.investfeed.global.holiday.HolidayService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class HoldingGradeScheduler(
    private val holdingGradeService: HoldingGradeService,
    private val holidayService: HolidayService,
    private val schedulerLogService: SchedulerLogService,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = SchedulerCron.HOLDING_GRADE, scheduler = "slowScheduler")
    fun scheduledHoldingGrade() {
        log.info { "HoldingGradeScheduler cron fired" }
        schedulerLogService.markFired(SchedulerName.HoldingGradeScheduler)
        if (holidayService.isHoliday()) {
            log.info { "HoldingGradeScheduler skipped: today is holiday" }
            return
        }

        if (schedulerLogService.isRunning(SchedulerName.RecommendScheduler)) {
            log.warn { "HoldingGradeScheduler skipped: RecommendScheduler 실행 중 (추천 완료 후 평가)" }
            return
        }

        if (schedulerLogService.isRunning(SchedulerName.BacktestBackfillScheduler)) {
            log.warn { "HoldingGradeScheduler skipped: BacktestBackfillScheduler 실행 중" }
            return
        }
        holdingGradeService.runHoldingGrade()
    }
}
