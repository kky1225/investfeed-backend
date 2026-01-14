package com.example.investfeed.domain.dashboard.dto.res

data class InvestorTradeRankListItem(
    var stkCd: String? = null,
    var rank: String? = null,
    var stkNm: String? = null,
    var pridStkpcFluRt: String? = null,
    var nettrdeAmt: String? = null
)