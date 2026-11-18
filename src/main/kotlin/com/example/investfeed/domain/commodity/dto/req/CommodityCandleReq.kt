package com.example.investfeed.domain.commodity.dto.req

import com.example.investfeed.kiwoom.chart.enum.CommodityChartType

data class CommodityCandleReq(
    var chartType: CommodityChartType = CommodityChartType.DAY,
    var cursor: String? = null,     // 앞선 응답의 nextCursor. 없으면 가장 최근 구간부터
)
