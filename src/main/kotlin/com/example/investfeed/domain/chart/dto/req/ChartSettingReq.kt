package com.example.investfeed.domain.chart.dto.req

data class ChartSettingReq(
    val hidden: List<String> = emptyList(),     // 꺼 둔 지표의 key (average, rsi, macd 등)
)
