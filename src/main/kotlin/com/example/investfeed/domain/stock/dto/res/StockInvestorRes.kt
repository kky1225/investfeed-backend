package com.example.investfeed.domain.stock.dto.res

data class StockInvestorRes(
    var investorTradeDailyList: List<StockInvestorListItem>? = null
)