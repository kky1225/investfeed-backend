package com.example.investfeed.kiwoom.stock.dto.res

data class StockInfoJumpListRes(
    var return_code: Int,
    var return_msg: String,
    var pric_jmpflu: List<StockInfoJumpList>? = null
)