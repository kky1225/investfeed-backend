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
        private const val HOLDING_GRADE_CRON_HOUR = 22
        private const val HOLDING_GRADE_CRON_MIN = 10
    }

    @Scheduled(cron = "0 50 8 * * *", scheduler = "slowScheduler")
    fun scheduledPaperTradeExec() {
        log.info { "PaperTradeExecScheduler cron fired" }
        if (holidayService.isHoliday()) {
            log.info { "PaperTradeExecScheduler skipped: today is holiday" }
            return
        }

        // 직전 거래일(월요일이면 금요일, 명절 다음 거래일이면 직전 거래일) 의 야간 잡 신선도 검증.
        // RecommendScheduler 22:00 / HoldingGradeScheduler 22:10 둘 다 직전 거래일 사이클에 성공해야 진행.
        val priorTradingDay = holidayService.lastTradingDay(LocalDate.now().minusDays(1))
        val recommendThreshold = priorTradingDay.atTime(RECOMMEND_CRON_HOUR, RECOMMEND_CRON_MIN)
        if (!schedulerLogService.isSucceededSince(SchedulerName.RecommendScheduler, recommendThreshold)) {
            log.error {
                "PaperTradeExecScheduler 중단: RecommendScheduler 가 $recommendThreshold 이후 성공한 적 없음. " +
                    "stock_pick 이 stale 데이터일 위험 — 매매 보류."
            }
            return
        }
        val holdingGradeThreshold = priorTradingDay.atTime(HOLDING_GRADE_CRON_HOUR, HOLDING_GRADE_CRON_MIN)
        if (!schedulerLogService.isSucceededSince(SchedulerName.HoldingGradeScheduler, holdingGradeThreshold)) {
            log.error {
                "PaperTradeExecScheduler 중단: HoldingGradeScheduler 가 $holdingGradeThreshold 이후 성공한 적 없음. " +
                    "holding_grade 가 stale 데이터일 위험 — 매매 보류."
            }
            return
        }

        paperTradeExecutionService.runPaperTradeExec()
    }
}
