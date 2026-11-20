package com.example.investfeed.common.util

import java.time.LocalTime
import java.time.ZoneId

/**
 * 날짜와 무관한 장 시각만 둔다.
 * 국내 정규장 개장·마감과 거기서 계산하는 시각(동시호가·NXT 시작·보유 마감 등)은 날마다 다를 수 있어
 * KrMarketSessionService(global/holiday) 에서 날짜별로 구한다 (수능일·새해 첫 거래일)
 */
object MarketTimeUtil {
    val US_ZONE: ZoneId = ZoneId.of("America/New_York")
    val US_OPEN: LocalTime = LocalTime.of(9, 30) // 미국 동부 기준 (서머타임 자동 반영)
    val US_CLOSE: LocalTime = LocalTime.of(16, 0)

    val NXT_CLOSE: LocalTime = LocalTime.of(20, 0)        // 애프터마켓 종료 — 장 시각이 바뀌는 날에도 같음
    val KRX_TRADE_CLOSE: LocalTime = LocalTime.of(20, 1)

    fun isUsOpen(now: LocalTime = LocalTime.now(US_ZONE)): Boolean =
        !now.isBefore(US_OPEN) && now.isBefore(US_CLOSE)

    fun isKrxTradeClose(now: LocalTime = LocalTime.now()): Boolean =
        !now.isBefore(KRX_TRADE_CLOSE)

    fun isNxtTradeClose(now: LocalTime = LocalTime.now()): Boolean =
        !now.isBefore(NXT_CLOSE)
}
