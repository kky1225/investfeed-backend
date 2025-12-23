package com.example.investfeed.domain.stock.dto.res

data class StockNewPriceListRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var ntl_pric: List<com.example.investfeed.domain.stock.dto.res.StockNewPriceList>? = null // 신고저가
)