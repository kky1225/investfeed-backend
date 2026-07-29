package com.example.investfeed.common.util

import java.time.LocalTime
import java.time.ZoneId

object MarketTimeUtil {
    val US_ZONE: ZoneId = ZoneId.of("America/New_York")
    val US_OPEN: LocalTime = LocalTime.of(9, 30) // 미국 동부 기준 (서머타임 자동 반영)
    val US_CLOSE: LocalTime = LocalTime.of(16, 0)

    val KRX_OPEN: LocalTime = LocalTime.of(9, 0)
    val KRX_CLOSE: LocalTime = LocalTime.of(15, 30)
    val NXT_OPEN: LocalTime = LocalTime.of(8, 0)
    val NXT_CLOSE: LocalTime = LocalTime.of(20, 0)
    val KRX_TRADE_CLOSE: LocalTime = LocalTime.of(15, 36)
    val STOCK_ALERT_START: LocalTime = LocalTime.of(8, 1)
    val KRX_HOLDING_CLOSE: LocalTime = LocalTime.of(15, 40)
    val PRE_MARKET_CALL_START: LocalTime = LocalTime.of(8, 50)
    val CLOSE_CALL_START: LocalTime = LocalTime.of(15, 20)
    val OVT_SINGLE_START: LocalTime = LocalTime.of(16, 0)
    val OVT_SINGLE_END: LocalTime = LocalTime.of(18, 0)

    fun isNxtOpen(now: LocalTime = LocalTime.now()): Boolean =
        !now.isBefore(NXT_OPEN) && now.isBefore(NXT_CLOSE)

    fun isUsOpen(now: LocalTime = LocalTime.now(US_ZONE)): Boolean =
        !now.isBefore(US_OPEN) && now.isBefore(US_CLOSE)

    fun isKrxOpen(now: LocalTime = LocalTime.now()): Boolean =
        !now.isBefore(KRX_OPEN) && now.isBefore(KRX_CLOSE)

    fun isKrxTradeClose(now: LocalTime = LocalTime.now()): Boolean =
        !now.isBefore(KRX_TRADE_CLOSE)

    fun isNxtTradeClose(now: LocalTime = LocalTime.now()): Boolean =
        !now.isBefore(NXT_CLOSE)

    fun isStockAlertTime(now: LocalTime = LocalTime.now()): Boolean =
        !now.isBefore(STOCK_ALERT_START) && now.isBefore(NXT_CLOSE)

    fun isKrxHoldingClose(now: LocalTime = LocalTime.now()): Boolean =
        !now.isBefore(KRX_HOLDING_CLOSE)

    fun isCallAuction(now: LocalTime = LocalTime.now()): Boolean =
        (!now.isBefore(PRE_MARKET_CALL_START) && now.isBefore(KRX_OPEN))
                || (!now.isBefore(CLOSE_CALL_START) && now.isBefore(KRX_CLOSE))

    fun isOvtSinglePrice(now: LocalTime = LocalTime.now()): Boolean =
        !now.isBefore(OVT_SINGLE_START) && now.isBefore(OVT_SINGLE_END)
}
