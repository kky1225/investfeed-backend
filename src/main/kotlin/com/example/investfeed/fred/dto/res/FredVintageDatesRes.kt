package com.example.investfeed.fred.dto.res

data class FredVintageDatesRes(
    val realtime_start: String? = null,
    val realtime_end: String? = null,
    val count: Int? = null,
    val vintage_dates: List<String>? = null,
)
