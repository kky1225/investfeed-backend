package com.example.investfeed.domain.chart.dto.res

data class ChartSettingRes(
    val hidden: List<String> = emptyList(),     // 꺼 둔 지표의 key. 저장한 적이 없으면 빈 목록(전부 켜짐)
)
