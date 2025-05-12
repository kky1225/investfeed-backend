package com.example.investfeed.kiwoom.stockinfo.dto.res

data class StockInfoListRes (
    var return_code: Int,
    var return_msg: String,
    var list: List<StockInfoList>? = null
)