package com.example.investfeed.domain.monitoring.enum

import com.example.investfeed.domain.monitoring.service.SchedulerType

enum class SchedulerName(
    val type: SchedulerType,
    val defaultTimeoutSec: Int,
    val label: String,
    val blockedOnHoliday: Boolean = false,
    val crons: List<String> = emptyList(),
) {
    // FAST
    PriceAlertScheduler(SchedulerType.FAST, 60, "매분", crons = listOf(SchedulerCron.PRICE_ALERT)),
    MarketIndexScheduler(SchedulerType.FAST, 60, "매분", crons = listOf(SchedulerCron.MARKET_INDEX)),
    MarketMacroScheduler(
        SchedulerType.FAST, 60, "매분(09:00~16:00)", blockedOnHoliday = true,
        crons = listOf(SchedulerCron.MARKET_MACRO_DURING, SchedulerCron.MARKET_MACRO_CLOSE),
    ),
    InvestorCloseMarketScheduler(SchedulerType.FAST, 60, "매분(15:36~21:00)", crons = listOf(SchedulerCron.INVESTOR_CLOSE_MARKET)),

    // SLOW (주기)
    RecommendTodayDirectionScheduler(
        SchedulerType.SLOW, 120, "매 5분(09:00~21:55)", blockedOnHoliday = true,
        crons = listOf(SchedulerCron.RECOMMEND_TODAY_DIRECTION),
    ),
    CalendarSyncScheduler(SchedulerType.SLOW, 300, "매 30분", crons = listOf(SchedulerCron.CALENDAR_SYNC)),
    GoalAlertScheduler(SchedulerType.SLOW, 60, "매시 정각", crons = listOf(SchedulerCron.GOAL_ALERT)),
    RebalancingAlertScheduler(SchedulerType.SLOW, 60, "매시 정각", crons = listOf(SchedulerCron.REBALANCING_ALERT)),

    // SLOW
    RecommendScheduler(SchedulerType.SLOW, 300, "매일 22:00", blockedOnHoliday = true, crons = listOf(SchedulerCron.RECOMMEND)),
    HoldingGradeScheduler(SchedulerType.SLOW, 600, "거래일 22:10", blockedOnHoliday = true, crons = listOf(SchedulerCron.HOLDING_GRADE)),
    BacktestBackfillScheduler(SchedulerType.SLOW, 600, "매일 22:30", crons = listOf(SchedulerCron.BACKTEST_BACKFILL)),
    HoldingSyncScheduler(SchedulerType.SLOW, 600, "매일 00:00", crons = listOf(SchedulerCron.HOLDING_SYNC)),
    HolidayRefreshScheduler(SchedulerType.SLOW, 120, "매월 1일 00:05", crons = listOf(SchedulerCron.HOLIDAY_REFRESH)),
    IndexDailyCloseScheduler(SchedulerType.SLOW, 120, "매일 00:10", crons = listOf(SchedulerCron.INDEX_DAILY_CLOSE)),
    SchedulerLogCleanupScheduler(SchedulerType.SLOW, 60, "매일 04:00", crons = listOf(SchedulerCron.SCHEDULER_LOG_CLEANUP)),
    StockMasterSyncScheduler(SchedulerType.SLOW, 600, "매일 05:00", crons = listOf(SchedulerCron.STOCK_MASTER_SYNC)),
    InterestSyncScheduler(SchedulerType.SLOW, 600, "매일 05:15", crons = listOf(SchedulerCron.INTEREST_SYNC)),
    IndexInvestorDailyScheduler(SchedulerType.SLOW, 120, "매일 07:00", crons = listOf(SchedulerCron.INDEX_INVESTOR_DAILY)),
    PaperTradeExecScheduler(
        SchedulerType.SLOW, 600, "거래일 08:50(장전)", blockedOnHoliday = true,
        crons = listOf(SchedulerCron.PAPER_TRADE_EXEC),
    ),
    PaperTradeSecondBuyScheduler(
        SchedulerType.SLOW, 300, "거래일 09:01(2차 매수)", blockedOnHoliday = true,
        crons = listOf(SchedulerCron.PAPER_TRADE_SECOND_BUY),
    ),
    ApiKeyExpiryScheduler(SchedulerType.SLOW, 60, "매일 09:00", crons = listOf(SchedulerCron.API_KEY_EXPIRY)),
    StockDividendScheduler(SchedulerType.SLOW, 60, "매일 13:30", crons = listOf(SchedulerCron.STOCK_DIVIDEND)),
}
