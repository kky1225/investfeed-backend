package com.example.investfeed.kiwoom.stock.entity.req

import com.example.investfeed.kiwoom.chart.enum.StockChartType

data class KiwoomStockInfoReq (
    var stk_cd: String, // 종목코드
    var chart_type: StockChartType = StockChartType.DAY
)