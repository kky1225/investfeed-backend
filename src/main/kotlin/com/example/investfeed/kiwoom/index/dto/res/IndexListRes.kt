package com.example.investfeed.kiwoom.index.dto.res

import com.example.investfeed.kiwoom.chart.dto.gold.res.GoldChartMinuteListRes
import com.example.investfeed.kiwoom.chart.dto.sect.res.SectChartMinuteListRes
import com.example.investfeed.kiwoom.gold.dto.rest.res.GoldPriceNowRes
import com.example.investfeed.kiwoom.sect.dto.rest.res.SectPriceNowRes

data class IndexListRes(
    var kospiPriceRes: SectPriceNowRes? = null,
    var kospiChartMinuteListRes: SectChartMinuteListRes? = null,
    var kosdacPriceRes: SectPriceNowRes? = null,
    var kosdacChartMinuteListRes: SectChartMinuteListRes? = null,
    var kospi200PriceRes: SectPriceNowRes? = null,
    var kospi200ChartMinuteListRes: SectChartMinuteListRes? = null,
    var goldPriceRes: GoldPriceNowRes? = null,
    var goldChartMinuteListRes: GoldChartMinuteListRes? = null,
)