package com.example.investfeed.domain.chart.dto.req

data class ChartSettingReq(
    val hidden: List<String> = emptyList(),     // 꺼 둔 지표의 key (average, rsi, macd 등)
    val flatByPrevClose: Boolean = false,       // 시가와 종가가 같은 캔들의 색을 전일 종가와 비교해 정할지
)
