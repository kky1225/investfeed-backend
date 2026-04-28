package com.example.investfeed.domain.commodity.dto.req

import com.example.investfeed.kiwoom.chart.enum.CommodityChartType

data class CommodityDetailReq(
    var chartType: CommodityChartType = CommodityChartType.DAY
)