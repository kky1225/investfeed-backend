package com.example.investfeed.domain.recommend.scheduler

import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.recommend.service.RecommendService
import com.example.investfeed.global.holiday.HolidayService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 추천 관련 스케줄러 진입점. @Scheduled 메서드를 RecommendService 와 분리해서
 * runRecommendStock / runRefreshTodayDirection 호출이 Spring AOP 프록시를 거치도록 함
 * (= @Transactional 정상 적용). 같은 클래스 self-invocation 이면 트랜잭션이 안 걸려서
 * @Modifying 쿼리(deleteByPickDateBetween 등)가 TransactionRequiredException 으로 터짐.
 */
@Component
class RecommendScheduler(
    private val recommendService: RecommendService,
    private val holidayService: HolidayService,
    private val schedulerLogService: SchedulerLogService,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 22 * * *", scheduler = "slowScheduler")
    fun scheduledRecommendStock() {
        log.info { "RecommendScheduler cron fired" }
        if (holidayService.isHoliday()) {
            log.info { "RecommendScheduler skipped: today is holiday" }
            return
        }
        if (schedulerLogService.isRunning(SchedulerName.RecommendTodayDirectionScheduler)) {
            log.warn { "RecommendScheduler skipped: RecommendTodayDirectionScheduler 실행 중 (stock_pick 충돌 방지)" }
            return
        }
        recommendService.runRecommendStock()
    }

    @Scheduled(cron = "0 */5 9-21 * * *", scheduler = "slowScheduler")
    fun scheduledRefreshTodayDirection() {
        log.info { "RecommendTodayDirectionScheduler cron fired" }
        if (holidayService.isHoliday()) {
            log.info { "RecommendTodayDirectionScheduler skipped: today is holiday" }
            return
        }
        if (schedulerLogService.isRunning(SchedulerName.RecommendScheduler)) {
            log.warn { "RecommendTodayDirectionScheduler skipped: RecommendScheduler 실행 중 (stock_pick 충돌 방지)" }
            return
        }
        recommendService.runRefreshTodayDirection()
    }
}
