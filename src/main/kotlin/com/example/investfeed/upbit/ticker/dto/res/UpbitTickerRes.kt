package com.example.investfeed.upbit.ticker.dto.res

import com.fasterxml.jackson.annotation.JsonProperty

data class UpbitTickerRes(
    val market: String? = null,
    val trade_date: String? = null,
    val trade_time: String? = null,
    val trade_date_kst: String? = null,
    val trade_time_kst: String? = null,
    val trade_timestamp: Long? = null,
    val opening_price: Double? = null,
    val high_price: Double? = null,
    val low_price: Double? = null,
    val trade_price: Double? = null,
    val prev_closing_price: Double? = null,
    val change: String? = null,
    val change_price: Double? = null,
    val change_rate: Double? = null,
    val signed_change_price: Double? = null,
    val signed_change_rate: Double? = null,
    val trade_volume: Double? = null,
    val acc_trade_price: Double? = null,
    val acc_trade_price_24h: Double? = null,
    val acc_trade_volume: Double? = null,
    val acc_trade_volume_24h: Double? = null,
    val highest_52_week_price: Double? = null,
    val highest_52_week_date: String? = null,
    val lowest_52_week_price: Double? = null,
    val lowest_52_week_date: String? = null,
    val timestamp: Long? = null,
)
