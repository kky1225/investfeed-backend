package com.example.investfeed.domain.stock.dto.res

data class StockJumpListRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var pric_jmpflu: List<com.example.investfeed.domain.stock.dto.res.StockJumpList>? = null // 가격급등락
)