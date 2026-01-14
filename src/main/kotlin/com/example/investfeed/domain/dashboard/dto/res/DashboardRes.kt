package com.example.investfeed.domain.dashboard.dto.res

data class DashboardRes(
    var indexList: List<DashboardIndexListItem>? = null,
    var investorTradeRankList: List<InvestorTradeRankListItem>? = null,
)