package com.example.investfeed.upbit.candle.dto.res

data class UpbitCandleDayRes(
    val market: String? = null,
    val candle_date_time_utc: String? = null,
    val candle_date_time_kst: String? = null,
    val opening_price: Double? = null,
    val high_price: Double? = null,
    val low_price: Double? = null,
    val trade_price: Double? = null,
    val timestamp: Long? = null,
    val candle_acc_trade_price: Double? = null,
    val candle_acc_trade_volume: Double? = null,
    val prev_closing_price: Double? = null,
    val change_price: Double? = null,
    val change_rate: Double? = null,
)
