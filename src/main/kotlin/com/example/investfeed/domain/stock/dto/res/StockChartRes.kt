package com.example.investfeed.domain.stock.dto.res

data class StockChartRes(
    var stockInfo: StockInfo? = null,
    var stockChartList: List<StockChart>? = null,
)
