package com.example.investfeed.domain.commodity.dto.req

import com.example.investfeed.kiwoom.chart.enum.CommodityChartType

data class CommodityDetailReq(
    var stkCd: String, // 종목코드 M04020000 금 99.99_1kg, M04020100 미니금 99.99_100g
    var chartType: CommodityChartType = CommodityChartType.DAY
)