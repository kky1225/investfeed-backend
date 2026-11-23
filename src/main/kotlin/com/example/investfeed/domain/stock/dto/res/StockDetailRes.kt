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
    var watchlistGroupIds: List<Long> = emptyList(), // 이 종목이 들어 있는 내 관심 그룹. 비어 있지 않으면 하트를 채운다
)