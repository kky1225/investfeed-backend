package com.example.investfeed.kiwoom.stock.dto.res

data class StockTradeDailyListRes (
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var daly_trde_dtl: List<StockTradeDailyList>? = null // 일별거래상세
)