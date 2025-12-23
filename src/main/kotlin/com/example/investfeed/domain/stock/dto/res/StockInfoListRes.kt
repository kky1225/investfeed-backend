package com.example.investfeed.domain.stock.dto.res

data class StockInfoListRes (
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var list: List<com.example.investfeed.domain.stock.dto.res.StockInfoList>? = null // 종목리스트
)