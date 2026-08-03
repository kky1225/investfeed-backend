package com.example.investfeed.domain.papertrade.scheduler

import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.papertrade.service.PaperTradeExecutionService
import com.example.investfeed.global.holiday.HolidayService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PaperTradeExecutionScheduler(
    private val paperTradeExecutionService: PaperTradeExecutionService,
    private val holidayService: HolidayService,
    private val schedulerLogService: SchedulerLogService,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val RECOMMEND_CRON_HOUR = 22
        private const val RECOMMEND_CRON_MIN = 0
    }

    @Scheduled(cron = "0 50 8 * * *", scheduler = "slowScheduler")
    fun scheduledPaperTradeExec() {
        log.info { "PaperTradeExecScheduler cron fired" }
        if (holidayService.isHoliday()) {
            log.info { "PaperTradeExecScheduler skipped: today is holiday" }
            return
        }

        val priorTradingDay = holidayService.lastTradingDay(LocalDate.now().minusDays(1))
        val recommendThreshold = priorTradingDay.atTime(RECOMMEND_CRON_HOUR, RECOMMEND_CRON_MIN)
        if (!schedulerLogService.isSucceededSince(SchedulerName.RecommendScheduler, recommendThreshold)) {
            log.error {
                "PaperTradeExecScheduler 중단: RecommendScheduler 가 $recommendThreshold 이후 성공한 적 없음. " +
                    "stock_pick 이 stale 데이터일 위험 — 매매 보류."
            }
            return
        }
        val recommendSuccessAt = schedulerLogService.lastSuccessAt(SchedulerName.RecommendScheduler)
        if (recommendSuccessAt == null || !schedulerLogService.isSucceededSince(SchedulerName.HoldingGradeScheduler, recommendSuccessAt)) {
            log.error {
                "PaperTradeExecScheduler 중단: HoldingGradeScheduler 가 추천 성공($recommendSuccessAt) 이후 성공한 적 없음. " +
                    "holding_grade 가 stale 데이터일 위험 — 매매 보류."
            }
            return
        }

        paperTradeExecutionService.runPaperTradeExec()
    }
}
