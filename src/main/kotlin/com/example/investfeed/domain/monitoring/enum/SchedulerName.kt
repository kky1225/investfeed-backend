package com.example.investfeed.domain.monitoring.enum

import com.example.investfeed.domain.monitoring.service.SchedulerType

enum class SchedulerName(
    val type: SchedulerType,
    val defaultTimeoutSec: Int,
    val label: String,
    val blockedOnHoliday: Boolean = false,
) {
    PriceAlertScheduler(SchedulerType.FAST, 60, "매분"),
    MarketIndexScheduler(SchedulerType.FAST, 60, "매분"),
    MarketMacroScheduler(SchedulerType.FAST, 60, "매분(09:00~16:00)", blockedOnHoliday = true),
    InvestorCloseMarketScheduler(SchedulerType.FAST, 60, "매분(15:36~21:00)"),
    GoalAlertScheduler(SchedulerType.SLOW, 60, "매시 정각"),
    RebalancingAlertScheduler(SchedulerType.SLOW, 60, "매시 정각"),
    StockDividendScheduler(SchedulerType.SLOW, 60, "매일 13:30"),
    ApiKeyExpiryScheduler(SchedulerType.SLOW, 60, "매일 09:00"),
    SchedulerLogCleanupScheduler(SchedulerType.SLOW, 60, "매일 04:00"),
    RecommendScheduler(SchedulerType.SLOW, 300, "매일 22:00", blockedOnHoliday = true),
    RecommendTodayDirectionScheduler(SchedulerType.SLOW, 120, "매 5분(09:00~21:55)", blockedOnHoliday = true),
    IndexInvestorDailyScheduler(SchedulerType.SLOW, 120, "매일 07:00"),
    HolidayRefreshScheduler(SchedulerType.SLOW, 120, "매월 1일 00:05"),
    CalendarSyncScheduler(SchedulerType.SLOW, 300, "매 30분"),
    InterestSyncScheduler(SchedulerType.SLOW, 600, "매일 05:15"),
    HoldingSyncScheduler(SchedulerType.SLOW, 600, "매일 00:00"),
}
