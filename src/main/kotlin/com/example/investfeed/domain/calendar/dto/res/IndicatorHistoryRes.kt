package com.example.investfeed.domain.calendar.dto.res

data class IndicatorHistoryRes(
    val code: String,
    val name: String,
    val unit: String,
    val chartType: String = "linear", // linear, stepAfter
    val frequency: String = "D",      // D, M, Q
    val data: List<IndicatorDataPoint>
)