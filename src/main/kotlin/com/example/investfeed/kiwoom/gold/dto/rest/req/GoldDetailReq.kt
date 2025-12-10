package com.example.investfeed.kiwoom.gold.dto.rest.req

import com.example.investfeed.kiwoom.chart.enum.IndexChartType

data class GoldDetailReq(
    var stk_cd: String, // 종목코드 M04020000 금 99.99_1kg, M04020100 미니금 99.99_100g
    var chart_type: IndexChartType = IndexChartType.DAY
)