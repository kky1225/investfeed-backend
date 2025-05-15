package com.example.investfeed.kiwoom.stock.dto.res

data class StockInfoTradeDailyRes (
    var return_code: Int,
    var return_msg: String,
    var daly_trde_dtl: List<StockInfoTradeDaily>? = null
)