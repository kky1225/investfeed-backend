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
import com.example.investfeed.domain.rebalancing.scheduler.RebalancingAlertScheduler
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

/**
 * 관리자가 SLOW 스케줄러를 수동으로 즉시 실행하도록 하는 서비스.
 *
 * - INTERRUPTED / FAILED 로 실패했을 때 다음 정규 스케줄까지 기다리지 않고 복구하는 용도.
 * - FAST 스케줄러(매분)는 어차피 곧 다시 도니 수동 실행 제공 안 함.
 * - 호출 스레드는 slowScheduler 풀에 위임 → 컨트롤러 응답은 즉시 반환, 실행은 백그라운드.
 * - 현재 실행 중(last_started_at > last_finished_at 이고 timeout 내)이면 거부 — 중복 실행 방지.
 */
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
