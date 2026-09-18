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
    const val HOLIDAY_REFRESH = "0 5 0 * * *"
    const val INDEX_DAILY_CLOSE = "0 10 0 * * *"
    const val SCHEDULER_LOG_CLEANUP = "0 0 4 * * *"
    const val STOCK_MASTER_SYNC = "0 0 5 * * *"
    const val INTEREST_SYNC = "0 15 5 * * *"
    const val INDEX_INVESTOR_DAILY = "0 0 7 * * *"
    const val API_KEY_EXPIRY = "0 0 9 * * *"
    const val STOCK_DIVIDEND = "0 30 13 * * *"

    // ── AI 비서 브리핑 (귀속일은 BriefingService 의 직전 마감 세션 규칙) ──
    const val BRIEFING_KR_PRE = "0 0 7 * * *"
    const val BRIEFING_KR_CLOSE = "0 5 16 * * MON-FRI"    // 마감 데이터 정리 여유 + 매크로 수집(16:00)과 분리
    const val BRIEFING_KR_HOLDINGS = "0 5 20 * * MON-FRI"    // 애프터마켓 종료(20:00) 후 국내 계좌 마감
    const val BRIEFING_US_CLOSE = "0 * 2-7 * * TUE-SAT"   // 매분 판정, NYSE 마감(KST)+10분에 1회
    const val BRIEFING_COIN_DAILY = "0 5 9 * * *"           // 업비트 일봉 09:00 마감 후
}
