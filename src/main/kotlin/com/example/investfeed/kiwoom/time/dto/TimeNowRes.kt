package com.example.investfeed.kiwoom.time.dto

data class TimeNowRes(
    var time: Long,
    var isMarketOpen: Boolean,
    var startMarketTime: Long,
    var endMarketTime: Long,
    var marketType: String? = null,
)