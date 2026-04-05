package com.example.investfeed.kiwoom.price.dto.req

data class KiwoomStockProgramTradeMinuteReq(
    var amt_qty_tp: String? = null, // 금액수량구분 1:금액, 2:수량
    var stk_cd: String? = null, // 종목코드
    var date: String? = null, // 날짜 YYYYMMDD
)
