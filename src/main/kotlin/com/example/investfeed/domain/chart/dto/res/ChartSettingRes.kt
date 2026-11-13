package com.example.investfeed.domain.chart.dto.res

data class ChartSettingRes(
    val hidden: List<String> = emptyList(),     // 꺼 둔 지표의 key. 저장한 적이 없으면 빈 목록(전부 켜짐)
    val flatByPrevClose: Boolean = false,       // 시가와 종가가 같은 캔들의 색 기준. false 면 상승색, true 면 전일 종가와 비교
)
