package com.example.investfeed.domain.stock.dto.res

data class StockDetailRes(
    var stockInfo: StockInfo? = null,
    var stockChartList: List<StockChart>? = null,
    var stockInvestorChartList: List<StockInvestorChart>? = null,
    var stockInvestorList: List<StockInvestor>? = null,
    var stockProgramList: List<StockProgram>? = null,
    var stockProgramChartList: List<StockProgramChart>? = null,
    var stockShortSellingList: List<StockShortSelling>? = null
)