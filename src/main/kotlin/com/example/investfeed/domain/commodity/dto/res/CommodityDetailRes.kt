package com.example.investfeed.domain.commodity.dto.res

data class CommodityDetailRes(
    var commodityInfo: CommodityInfo? = null,
    var commodityChartList: List<CommodityChart>? = null,
)