package com.example.investfeed.domain.index.dto.req

import com.example.investfeed.kiwoom.chart.enum.IndexChartType

data class IndexDetailReq(
    var chart_type: IndexChartType = IndexChartType.DAY
)