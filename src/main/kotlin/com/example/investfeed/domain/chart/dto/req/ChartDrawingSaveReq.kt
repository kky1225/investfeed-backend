package com.example.investfeed.domain.chart.dto.req

import com.example.investfeed.domain.chart.dto.ChartDrawingItem

data class ChartDrawingSaveReq(
    val drawings: List<ChartDrawingItem> = emptyList(),     // 이 종목의 선 전체. 빈 목록이면 모두 삭제
)
