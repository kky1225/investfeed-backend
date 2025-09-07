package com.example.investfeed.kiwoom.time.dto.res

data class TimeNowRes(
    var time: Long,
    var marketType: String,
    var isMarketOpen: Boolean,
    var exchangeType: String? = null,
    var startMarketTime: Long,
    var endMarketTime: Long,
)