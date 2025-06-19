package com.example.investfeed.kiwoom.index.dto.res

import com.example.investfeed.kiwoom.chart.dto.index.res.SectChartMinuteListRes
import com.example.investfeed.kiwoom.sect.dto.rest.res.SectPriceNowRes

data class IndexListRes(
    var kospiPriceRes: SectPriceNowRes? = null,
    var kospiChartMinuteListRes: SectChartMinuteListRes? = null,
    var kosdacPriceRes: SectPriceNowRes? = null,
    var kosdacChartMinuteListRes: SectChartMinuteListRes? = null,
    var kospi200PriceRes: SectPriceNowRes? = null,
    var kospi200ChartMinuteListRes: SectChartMinuteListRes? = null,
)