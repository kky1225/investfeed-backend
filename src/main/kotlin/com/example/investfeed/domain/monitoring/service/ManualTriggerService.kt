package com.example.investfeed.domain.monitoring.service

import com.example.investfeed.domain.calendar.scheduler.CalendarSyncScheduler
import com.example.investfeed.domain.dividend.scheduler.StockDividendScheduler
import com.example.investfeed.domain.goal.scheduler.GoalAlertScheduler
import com.example.investfeed.domain.holding.scheduler.HoldingSyncScheduler
import com.example.investfeed.domain.index.scheduler.IndexInvestorDailyScheduler
import com.example.investfeed.domain.interest.scheduler.InterestSyncScheduler
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.repository.SchedulerStatusRepository
import com.example.investfeed.domain.monitoring.scheduler.SchedulerLogCleanupScheduler
import com.example.investfeed.domain.notification.scheduler.ApiKeyExpiryScheduler
import com.example.investfeed.domain.index.service.IndexService
import com.example.investfeed.domain.papertrade.service.HoldingGradeService
import com.example.investfeed.domain.papertrade.service.PaperTradeExecutionService
import com.example.investfeed.domain.rebalancing.scheduler.RebalancingAlertScheduler
import com.example.investfeed.domain.stock.scheduler.StockMasterSyncScheduler
import com.example.investfeed.domain.recommend.service.BacktestBackfillService
import com.example.investfeed.domain.recommend.service.RecommendService
import com.example.investfeed.global.holiday.HolidayService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

@Service
class ManualTriggerService(
    @Qualifier("slowScheduler") private val taskScheduler: TaskScheduler,
    private val schedulerStatusRepository: SchedulerStatusRepository,
    private val goalAlertScheduler: GoalAlertScheduler,
    private val rebalancingAlertScheduler: RebalancingAlertScheduler,
    private val calendarSyncScheduler: CalendarSyncScheduler,
    private val recommendService: RecommendService,
    private val backtestBackfillService: BacktestBackfillService,
    private val holidayService: HolidayService,
    private val holdingSyncScheduler: HoldingSyncScheduler,
    private val stockDividendScheduler: StockDividendScheduler,
    private val indexInvestorDailyScheduler: IndexInvestorDailyScheduler,
    private val apiKeyExpiryScheduler: ApiKeyExpiryScheduler,
    private val schedulerLogCleanupScheduler: SchedulerLogCleanupScheduler,
    private val interestSyncScheduler: InterestSyncScheduler,
    private val stockMasterSyncScheduler: StockMasterSyncScheduler,
    private val holdingGradeService: HoldingGradeService,
    private val paperTradeExecutionService: PaperTradeExecutionService,
    private val indexService: IndexService,
) {
    private val log = KotlinLogging.logger {}

    private val runnableMap: Map<String, () -> Unit> by lazy {
        mapOf(
            "GoalAlertScheduler"           to { goalAlertScheduler.checkGoals() },
            "RebalancingAlertScheduler"    to { rebalancingAlertScheduler.checkRebalancing() },
            "CalendarSyncScheduler"        to { calendarSyncScheduler.syncCalendarData() },
            "RecommendScheduler"           to { recommendService.runRecommendStock() },
            "RecommendTodayDirectionScheduler" to { recommendService.runRefreshTodayDirection() },
            "BacktestBackfillScheduler"    to { backtestBackfillService.runBackfill() },
            "HolidayRefreshScheduler"      to { holidayService.refreshHolidays() },
            "HoldingSyncScheduler"         to { holdingSyncScheduler.syncAllHoldings() },
            "StockDividendScheduler"       to { stockDividendScheduler.collectDaily() },
            "IndexInvestorDailyScheduler"  to { indexInvestorDailyScheduler.collectDaily() },
            "ApiKeyExpiryScheduler"        to { apiKeyExpiryScheduler.checkApiKeyExpiry() },
            "SchedulerLogCleanupScheduler" to { schedulerLogCleanupScheduler.cleanup() },
            "InterestSyncScheduler"        to { interestSyncScheduler.syncAllStkNm() },
            "StockMasterSyncScheduler"     to { stockMasterSyncScheduler.syncStockMaster() },
            "HoldingGradeScheduler"        to { holdingGradeService.runHoldingGrade() },
            "PaperTradeExecScheduler"      to { paperTradeExecutionService.runPaperTradeExec() },
            "IndexDailyCloseScheduler"     to { indexService.runCollectIndexClose() },
        )
    }

    fun trigger(schedulerName: String, force: Boolean = false) {
        val runnable = runnableMap[schedulerName]
            ?: throw IllegalArgumentException("수동 실행을 지원하지 않는 스케줄러: $schedulerName")

        val schedulerEnum = runCatching { SchedulerName.valueOf(schedulerName) }.getOrNull()
        if (schedulerEnum?.blockedOnHoliday == true && holidayService.isHoliday() && !force) {
            throw IllegalStateException(
                "휴일에는 실행할 수 없는 스케줄러입니다: $schedulerName (force=true 로 우회 가능)"
            )
        }
        if (force && schedulerEnum?.blockedOnHoliday == true && holidayService.isHoliday()) {
            log.info { "[manual-trigger] $schedulerName 휴일 강제 실행 (force=true) — 직전 거래일 기준 갱신" }
        }

        runningStatus(schedulerName)?.let { (elapsed, timeoutSec) ->
            throw IllegalStateException(
                "스케줄러가 이미 실행 중입니다: $schedulerName (경과 ${elapsed}s, timeout ${timeoutSec}s)"
            )
        }

        if (schedulerName == SchedulerName.RecommendTodayDirectionScheduler.name
            && runningStatus(SchedulerName.RecommendScheduler.name) != null) {
            throw IllegalStateException("RecommendScheduler 실행이 끝난 후 시도하세요. (현재 실행 중)")
        }

        log.info { "[manual-trigger] $schedulerName 수동 실행 요청 — 백그라운드 예약" }
        taskScheduler.schedule(runnable, Instant.now())
    }

    private fun runningStatus(schedulerName: String): Pair<Long, Int>? {
        val status = schedulerStatusRepository.findById(schedulerName).orElse(null) ?: return null
        val started = status.lastStartedAt ?: return null
        val finished = status.lastFinishedAt
        if (finished != null && !finished.isBefore(started)) return null
        val elapsed = Duration.between(started, LocalDateTime.now()).seconds
        return if (elapsed <= status.timeoutSec) elapsed to status.timeoutSec else null
    }
}
