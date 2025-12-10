package com.example.investfeed.kiwoom.stock.dto.req

import com.example.investfeed.kiwoom.chart.enum.StockChartType

data class StockDetailReq(
    var stk_cd: String, // 종목코드
    var chart_type: StockChartType = StockChartType.DAY
)