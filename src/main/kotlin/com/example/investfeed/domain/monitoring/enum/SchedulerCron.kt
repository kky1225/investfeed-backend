package com.example.investfeed.domain.monitoring.enum

/**
 * 시각 기준 cron 은 0초가 아니라 1초에 시작한다.
 * Spring 스케줄러가 예정보다 최대 0.6초가량 일찍 시작하는 경우가 있어, 0초에 두면 날짜·장 시각 판단이 이전 시각으로 잡힌다.
 *
 * 국내 장 시각은 날마다 다를 수 있다 (수능일 10:00~16:30, 새해 첫 거래일 10:00 개장 — KrMarketSessionService).
 * 그래서 국내 장 시각을 따라 도는 작업은 cron(@Scheduled)이 아니라 KrSessionTaskScheduler 가 그날 장 시각으로 예약한다.
 * - 장중 반복(지수 알림·서킷브레이커 구독/판정·매크로 수집): 거래일 개장부터 마감(매크로는 마감+30분)까지만. 여기에 cron 없음
 * - 하루 한 번(모의투자 1차·2차, 국내 마감 브리핑): 아래 평소 시각 cron 에 개장·마감이 밀린 만큼 더한 시각.
 *   평소 시각 cron 은 예약 계산과 실행 누락 판정의 기준으로만 쓴다
 */
object SchedulerCron {
    // ─────────── FAST ───────────
    const val PRICE_ALERT = "1 * * * * *"
    const val MARKET_INDEX = "1 * * * * *"
    const val INVESTOR_CLOSE_MARKET = "1 * * * * *"
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
    const val PAPER_TRADE_EXEC = "1 50 8 * * *"         // 기준 시각: 개장 10분 전 (수능일 09:50)
    const val PAPER_TRADE_SECOND_BUY = "1 1 9 * * *"    // 기준 시각: 개장 1분 후 (수능일 10:01)

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
    const val BRIEFING_KR_CLOSE = "1 5 16 * * MON-FRI"    // 기준 시각: 마감 35분 후 (수능일 17:05). 마감 데이터 정리 여유 + 매크로 마감 수집(마감+30분)과 분리
    const val BRIEFING_KR_HOLDINGS = "1 5 20 * * MON-FRI"    // 애프터마켓 종료(20:00) 후 국내 계좌 마감
    const val BRIEFING_US_CLOSE = "1 * 2-7 * * TUE-SAT"   // 매분 판정, NYSE 마감(KST)+10분에 1회
    const val BRIEFING_COIN_DAILY = "1 5 9 * * *"           // 업비트 일봉 09:00 마감 후

    // ── AI 비서 장중 알림 (2단계) ──
    const val ASSISTANT_INDEX_ALERT_US = "1 * 22,23,0-6 * * *"    // 매분 판정, 코드에서 미국 세션(09:30 ET~폐장)만 (FAST)
    const val TELEGRAM_POLL = "*/5 * * * * *"                     // 텔레그램 getUpdates 5초 폴링 (FAST). 같은 작업은 겹쳐 실행되지 않음
}
