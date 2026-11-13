package com.example.investfeed.domain.stock.dto.req

import com.example.investfeed.kiwoom.chart.enum.StockChartType

data class StockCandleReq(
    var chartType: StockChartType = StockChartType.DAY,
    var cursor: String? = null,     // 앞선 응답의 nextCursor. 없으면 가장 최근 구간부터
)
