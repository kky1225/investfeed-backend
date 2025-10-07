package com.example.investfeed.kiwoom.gold.dto.rest.req

import com.example.investfeed.kiwoom.chart.enum.ChartType

data class GoldDetailReq(
    var stk_cd: String, // M04020000
    var chart_type: ChartType = ChartType.DAY
)