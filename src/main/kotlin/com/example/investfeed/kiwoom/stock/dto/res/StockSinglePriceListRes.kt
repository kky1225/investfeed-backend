package com.example.investfeed.kiwoom.stock.dto.res

data class StockSinglePriceListRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var bid_req_base_tm: List<StockSinglePriceList>? = null // 호가잔량기준시간
)