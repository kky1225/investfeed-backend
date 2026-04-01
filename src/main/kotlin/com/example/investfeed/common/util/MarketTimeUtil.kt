package com.example.investfeed.common.util

import java.time.LocalTime

object MarketTimeUtil {
    val KRX_OPEN: LocalTime = LocalTime.of(9, 0)
    val KRX_CLOSE: LocalTime = LocalTime.of(15, 30)
    val NXT_OPEN: LocalTime = LocalTime.of(8, 0)
    val NXT_CLOSE: LocalTime = LocalTime.of(20, 0)
    val KRX_TRADE_CLOSE: LocalTime = LocalTime.of(15, 34)
    val STOCK_ALERT_START: LocalTime = LocalTime.of(8, 1)
    val KRX_HOLDING_CLOSE: LocalTime = LocalTime.of(15, 40)

    fun isNxtOpen(now: LocalTime = LocalTime.now()): Boolean =
        !now.isBefore(NXT_OPEN) && now.isBefore(NXT_CLOSE)

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
}
