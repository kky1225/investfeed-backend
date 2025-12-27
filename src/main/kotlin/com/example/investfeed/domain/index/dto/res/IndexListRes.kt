package com.example.investfeed.domain.index.dto.res

import com.example.investfeed.kiwoom.chart.dto.gold.res.GoldChartMinuteListRes
import com.example.investfeed.kiwoom.chart.dto.sect.res.SectChartMinuteListRes
import com.example.investfeed.kiwoom.gold.dto.rest.res.GoldPriceNowRes
import com.example.investfeed.kiwoom.sect.dto.res.KiwoomSectPriceNowRes

data class IndexListRes(
    var kospiPriceRes: KiwoomSectPriceNowRes? = null,
    var kospiChartMinuteListRes: SectChartMinuteListRes? = null,
    var kosdacPriceRes: KiwoomSectPriceNowRes? = null,
    var kosdacChartMinuteListRes: SectChartMinuteListRes? = null,
    var kospi200PriceRes: KiwoomSectPriceNowRes? = null,
    var kospi200ChartMinuteListRes: SectChartMinuteListRes? = null,
    var goldPriceRes: GoldPriceNowRes? = null,
    var goldChartMinuteListRes: GoldChartMinuteListRes? = null,
)