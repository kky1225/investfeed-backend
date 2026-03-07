package com.example.investfeed.kiwoom.price.dto.req

data class KiwoomStockProgramTradeDayReq(
    var amt_qty_tp: String? = null, //금액수량구분 1:금액, 2:수량
    var stk_cd: String, //종목코드 거래소별 종목코드 (KRX:039490,NXT:039490_NX,SOR:039490_AL)
    var date: String? = null, //날짜 YYYYMMDD
)