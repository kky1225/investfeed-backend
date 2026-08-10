package com.example.investfeed.domain.stock.dto.res

import com.example.investfeed.domain.dividend.dto.res.StockDividendRes

data class StockDetailRes(
    var stockInfo: StockInfo? = null,
    var stockChartList: List<StockChart>? = null,
    var stockInvestorChartList: List<StockInvestorChart>? = null,
    var stockInvestorList: List<StockInvestor>? = null,
    var stockProgramList: List<StockProgram>? = null,
    var stockProgramChartList: List<StockProgramChart>? = null,
    var stockShortSellingList: List<StockShortSelling>? = null,
    var dailyPriceList: List<StockDailyPrice>? = null,
    var dividendList: List<StockDividendRes>? = null,
    var viList: List<StockVi>? = null,
)