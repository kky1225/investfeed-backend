package com.example.investfeed.domain.index.dto.req

import com.example.investfeed.kiwoom.chart.enum.IndexChartType

data class IndexCandleReq(
    var chartType: IndexChartType = IndexChartType.DAY,
    var cursor: String? = null,     // 앞선 응답의 nextCursor. 없으면 가장 최근 구간부터
)
