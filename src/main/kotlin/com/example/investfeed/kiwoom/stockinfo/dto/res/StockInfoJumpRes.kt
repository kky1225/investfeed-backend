package com.example.investfeed.kiwoom.stockinfo.dto.res

data class StockInfoJumpRes(
    var return_code: Int,
    var return_msg: String,
    var pric_jmpflu: List<StockInfoJumpList>? = null
)