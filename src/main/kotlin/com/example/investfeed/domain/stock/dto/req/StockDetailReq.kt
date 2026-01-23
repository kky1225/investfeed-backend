package com.example.investfeed.domain.stock.dto.req

import com.example.investfeed.kiwoom.chart.enum.StockChartType

data class StockDetailReq(
    var stkCd: String, // 종목코드
    var chartType: StockChartType = StockChartType.DAY
)