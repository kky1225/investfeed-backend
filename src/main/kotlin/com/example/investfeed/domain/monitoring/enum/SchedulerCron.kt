package com.example.investfeed.domain.monitoring.enum

object SchedulerCron {
    // ─────────── FAST ───────────
    const val PRICE_ALERT = "0 * * * * *"
    const val MARKET_INDEX = "0 * * * * *"
    const val INVESTOR_CLOSE_MARKET = "0 * * * * *"
    const val MARKET_MACRO_DURING = "0 * 9-15 * * MON-FRI"
    const val MARKET_MACRO_CLOSE = "0 0 16 * * MON-FRI"

    // ─────────── SLOW (주기) ───────────
    const val RECOMMEND_TODAY_DIRECTION = "0 */5 9-21 * * *"
    const val CALENDAR_SYNC = "0 */30 * * * *"
    const val GOAL_ALERT = "0 0 * * * *"
    const val REBALANCING_ALERT = "0 0 * * * *"

    // ─────────── SLOW (매매 사이클) ───────────
    const val RECOMMEND = "0 0 22 * * *"
    const val HOLDING_GRADE = "0 10 22 * * *"
    const val BACKTEST_BACKFILL = "0 30 22 * * *"
    const val PAPER_TRADE_EXEC = "0 50 8 * * *"
    const val PAPER_TRADE_SECOND_BUY = "0 1 9 * * *"

    // ─────────── SLOW (일/월 단위) ───────────
    const val HOLDING_SYNC = "0 0 0 * * *"
    const val HOLIDAY_REFRESH = "0 5 0 1 * *"
    const val INDEX_DAILY_CLOSE = "0 10 0 * * *"
    const val SCHEDULER_LOG_CLEANUP = "0 0 4 * * *"
    const val STOCK_MASTER_SYNC = "0 0 5 * * *"
    const val INTEREST_SYNC = "0 15 5 * * *"
    const val INDEX_INVESTOR_DAILY = "0 0 7 * * *"
    const val API_KEY_EXPIRY = "0 0 9 * * *"
    const val STOCK_DIVIDEND = "0 30 13 * * *"
}
