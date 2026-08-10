package com.example.investfeed.domain.monitoring.enum

import com.example.investfeed.domain.monitoring.service.SchedulerType

enum class SchedulerName(
    val type: SchedulerType,
    val defaultTimeoutSec: Int,
    val label: String,
    val blockedOnHoliday: Boolean = false,
) {
    // FAST
    PriceAlertScheduler(SchedulerType.FAST, 60, "매분"),
    MarketIndexScheduler(SchedulerType.FAST, 60, "매분"),
    MarketMacroScheduler(SchedulerType.FAST, 60, "매분(09:00~16:00)", blockedOnHoliday = true),
    InvestorCloseMarketScheduler(SchedulerType.FAST, 60, "매분(15:36~21:00)"),

    // SLOW (주기)
    RecommendTodayDirectionScheduler(SchedulerType.SLOW, 120, "매 5분(09:00~21:55)", blockedOnHoliday = true),
    CalendarSyncScheduler(SchedulerType.SLOW, 300, "매 30분"),
    GoalAlertScheduler(SchedulerType.SLOW, 60, "매시 정각"),
    RebalancingAlertScheduler(SchedulerType.SLOW, 60, "매시 정각"),

    // SLOW
    RecommendScheduler(SchedulerType.SLOW, 300, "매일 22:00", blockedOnHoliday = true),
    HoldingGradeScheduler(SchedulerType.SLOW, 600, "거래일 22:10", blockedOnHoliday = true),
    BacktestBackfillScheduler(SchedulerType.SLOW, 600, "매일 22:30"),
    HoldingSyncScheduler(SchedulerType.SLOW, 600, "매일 00:00"),
    HolidayRefreshScheduler(SchedulerType.SLOW, 120, "매월 1일 00:05"),
    IndexDailyCloseScheduler(SchedulerType.SLOW, 120, "매일 00:10"),
    SchedulerLogCleanupScheduler(SchedulerType.SLOW, 60, "매일 04:00"),
    StockMasterSyncScheduler(SchedulerType.SLOW, 600, "매일 05:00"),
    InterestSyncScheduler(SchedulerType.SLOW, 600, "매일 05:15"),
    IndexInvestorDailyScheduler(SchedulerType.SLOW, 120, "매일 07:00"),
    PaperTradeExecScheduler(SchedulerType.SLOW, 600, "거래일 08:50(장전)", blockedOnHoliday = true),
    ApiKeyExpiryScheduler(SchedulerType.SLOW, 60, "매일 09:00"),
    StockDividendScheduler(SchedulerType.SLOW, 60, "매일 13:30"),
}
