package com.example.investfeed.domain.monitoring.enum

/**
 * 시각 기준 cron 은 0초가 아니라 1초에 시작한다.
 * Spring 스케줄러가 예정보다 최대 0.6초가량 일찍 시작하는 경우가 있어, 0초에 두면 날짜·장 시각 판단이 이전 시각으로 잡힌다.
 */
object SchedulerCron {
    // ─────────── FAST ───────────
    const val PRICE_ALERT = "1 * * * * *"
    const val MARKET_INDEX = "1 * * * * *"
    const val INVESTOR_CLOSE_MARKET = "1 * * * * *"
    const val MARKET_MACRO_DURING = "1 * 9-15 * * MON-FRI"
    const val MARKET_MACRO_CLOSE = "1 0 16 * * MON-FRI"
    const val KIWOOM_SOCKET_CLEANUP = "1 * * * * *"
    const val TOSS_SOCKET_CLEANUP = "1 * * * * *"
    const val STREAM_HEARTBEAT = "*/30 * * * * *"
    // ─────────── SLOW (주기) ───────────
    const val RECOMMEND_TODAY_DIRECTION = "1 */5 9-21 * * *"
    const val CALENDAR_SYNC = "1 */30 * * * *"
    const val GOAL_ALERT = "1 0 * * * *"
    const val REBALANCING_ALERT = "1 0 * * * *"

    // ─────────── SLOW (매매 사이클) ───────────
    const val RECOMMEND = "1 0 22 * * *"
    const val HOLDING_GRADE = "1 10 22 * * *"
    const val BACKTEST_BACKFILL = "1 30 22 * * *"
    const val PAPER_TRADE_EXEC = "1 50 8 * * *"
    const val PAPER_TRADE_SECOND_BUY = "1 1 9 * * *"

    // ─────────── SLOW (일/월 단위) ───────────
    const val HOLDING_SYNC = "1 0 0 * * *"
    const val HOLIDAY_REFRESH = "1 5 0 * * *"
    const val INDEX_DAILY_CLOSE = "1 10 0 * * *"
    const val API_CALL_STAT_ROLLUP = "1 0 0 * * *"
    const val SCHEDULER_LOG_CLEANUP = "1 0 4 * * *"
    const val STOCK_MASTER_SYNC = "1 0 5 * * *"
    const val WATCHLIST_SYNC = "1 15 5 * * *"
    const val INDEX_INVESTOR_DAILY = "1 0 7 * * *"
    const val API_KEY_EXPIRY = "1 0 9 * * *"
    const val STOCK_DIVIDEND = "1 30 13 * * *"

    // ── AI 비서 브리핑 (귀속일은 BriefingService 의 직전 마감 세션 규칙) ──
    const val BRIEFING_KR_PRE = "1 0 7 * * *"
    const val BRIEFING_KR_CLOSE = "1 5 16 * * MON-FRI"    // 마감 데이터 정리 여유 + 매크로 수집(16:00)과 분리
    const val BRIEFING_KR_HOLDINGS = "1 5 20 * * MON-FRI"    // 애프터마켓 종료(20:00) 후 국내 계좌 마감
    const val BRIEFING_US_CLOSE = "1 * 2-7 * * TUE-SAT"   // 매분 판정, NYSE 마감(KST)+10분에 1회
    const val BRIEFING_COIN_DAILY = "1 5 9 * * *"           // 업비트 일봉 09:00 마감 후

    // ── AI 비서 장중 알림 (2단계) ──
    const val ASSISTANT_INDEX_ALERT_KR = "1 * 9-15 * * MON-FRI"   // 매분 판정, 코드에서 09:00~15:30·휴장 제외 (FAST)
    const val ASSISTANT_INDEX_ALERT_US = "1 * 22,23,0-6 * * *"    // 매분 판정, 코드에서 미국 세션(09:30 ET~폐장)만 (FAST)
    const val TELEGRAM_POLL = "*/5 * * * * *"                     // 텔레그램 getUpdates 5초 폴링 (FAST). 같은 작업은 겹쳐 실행되지 않음

    // ── 서킷브레이커 (지수 실시간 판정) ──
    const val CIRCUIT_BREAKER_SUBSCRIBE = "1 * 9-15 * * MON-FRI"  // 매분 0J 상시 구독 보장 + 수신 간격 로그, 코드에서 09:00~15:30·휴장 제외 (FAST)
    const val CIRCUIT_BREAKER_CHECK = "* * 9-15 * * MON-FRI"      // 매초 1분 유지 판정, 코드에서 09:00~15:30·휴장 제외 (FAST)
}
