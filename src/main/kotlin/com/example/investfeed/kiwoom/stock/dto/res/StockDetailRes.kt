package com.example.investfeed.kiwoom.stock.dto.res

data class StockDetailRes(
    var stockInfo: StockInfo? = null,
    var stockChartList: List<StockChart>? = null,
    var stockInvestorList: List<StockInvestor>? = null
)