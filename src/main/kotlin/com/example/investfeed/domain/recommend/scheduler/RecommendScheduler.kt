package com.example.investfeed.domain.recommend.scheduler

import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.recommend.service.RecommendService
import com.example.investfeed.global.holiday.HolidayService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RecommendScheduler(
    private val recommendService: RecommendService,
    private val holidayService: HolidayService,
    private val schedulerLogService: SchedulerLogService,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = SchedulerCron.RECOMMEND, scheduler = "slowScheduler")
    fun scheduledRecommendStock() {
        schedulerLogService.markFired(SchedulerName.RecommendScheduler)
        if (holidayService.isHoliday()) {
            schedulerLogService.skip(SchedulerName.RecommendScheduler, "휴일")
            return
        }
        if (schedulerLogService.isRunning(SchedulerName.RecommendTodayDirectionScheduler)) {
            log.warn { "RecommendScheduler skipped: RecommendTodayDirectionScheduler 실행 중 (stock_pick 충돌 방지)" }
            return
        }
        recommendService.runRecommendStock()
    }

    @Scheduled(cron = SchedulerCron.RECOMMEND_TODAY_DIRECTION, scheduler = "slowScheduler")
    fun scheduledRefreshTodayDirection() {
        schedulerLogService.markFired(SchedulerName.RecommendTodayDirectionScheduler)
        if (holidayService.isHoliday()) {
            schedulerLogService.skip(SchedulerName.RecommendTodayDirectionScheduler, "휴일")
            return
        }
        if (schedulerLogService.isRunning(SchedulerName.RecommendScheduler)) {
            log.warn { "RecommendTodayDirectionScheduler skipped: RecommendScheduler 실행 중 (stock_pick 충돌 방지)" }
            return
        }
        recommendService.runRefreshTodayDirection()
    }
}
