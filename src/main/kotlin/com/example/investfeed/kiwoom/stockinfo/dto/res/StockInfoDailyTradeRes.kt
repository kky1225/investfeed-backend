package com.example.investfeed.kiwoom.stockinfo.dto.res

data class StockInfoDailyTradeRes (
    var return_code: Int,
    var return_msg: String,
    var daly_trde_dtl: List<StockInfoDailyTrade>? = null
)