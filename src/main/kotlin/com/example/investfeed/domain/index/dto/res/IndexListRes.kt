package com.example.investfeed.domain.index.dto.res

import com.example.investfeed.kiwoom.chart.dto.gold.res.GoldChartMinuteListRes
import com.example.investfeed.kiwoom.gold.dto.rest.res.GoldPriceNowRes

data class IndexListRes(
    var indexList: List<IndexListItem>,
    var goldPriceRes: GoldPriceNowRes? = null,
    var goldChartMinuteListRes: GoldChartMinuteListRes? = null,
)