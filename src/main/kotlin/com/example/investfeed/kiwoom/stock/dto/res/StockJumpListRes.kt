package com.example.investfeed.kiwoom.stock.dto.res

data class StockJumpListRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var pric_jmpflu: List<StockJumpList>? = null // 가격급등락
)