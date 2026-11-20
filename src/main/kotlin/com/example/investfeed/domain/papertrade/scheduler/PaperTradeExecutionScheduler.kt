package com.example.investfeed.domain.papertrade.scheduler

import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.papertrade.service.PaperTradeExecutionService
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.global.holiday.KrSessionAnchor
import com.example.investfeed.global.holiday.KrSessionTaskScheduler
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PaperTradeExecutionScheduler(
    private val paperTradeExecutionService: PaperTradeExecutionService,
    private val holidayService: HolidayService,
    private val krSessionTaskScheduler: KrSessionTaskScheduler,
    private val schedulerLogService: SchedulerLogService,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val RECOMMEND_CRON_HOUR = 22
        private const val RECOMMEND_CRON_MIN = 0
    }

    /** 평소 08:50(개장 10분 전) / 09:01(개장 1분 후). 수능일·새해 첫 거래일처럼 개장이 늦은 날은 그만큼 늦춰 실행 */
    @PostConstruct
    fun registerTasks() {
        krSessionTaskScheduler.registerDaily(
            SchedulerName.PaperTradeExecScheduler.name, SchedulerCron.PAPER_TRADE_EXEC, KrSessionAnchor.OPEN, ::scheduledPaperTradeExec,
        )
        krSessionTaskScheduler.registerDaily(
            SchedulerName.PaperTradeSecondBuyScheduler.name, SchedulerCron.PAPER_TRADE_SECOND_BUY, KrSessionAnchor.OPEN, ::scheduledSecondPhaseBuys,
        )
    }

    fun scheduledPaperTradeExec() {
        schedulerLogService.markFired(SchedulerName.PaperTradeExecScheduler)
        if (holidayService.isHoliday()) {
            schedulerLogService.skip(SchedulerName.PaperTradeExecScheduler, "휴일")
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

    fun scheduledSecondPhaseBuys() {
        schedulerLogService.markFired(SchedulerName.PaperTradeSecondBuyScheduler)
        if (holidayService.isHoliday()) {
            schedulerLogService.skip(SchedulerName.PaperTradeSecondBuyScheduler, "휴일")
            return
        }
        paperTradeExecutionService.runSecondPhaseBuys()
    }
}
