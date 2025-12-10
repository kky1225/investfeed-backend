package com.example.investfeed.kiwoom.stock.entity.req

data class KiwoomStockInvestorReq(
    var dt: String? = null, // 일자 YYYYMMDD
    var stk_cd: String? = null, // 종목코드 거래소별 종목코드(KRX:039490,NXT:039490_NX,SOR:039490_AL)
    var amt_qty_tp: String? = null, // 금액수량구분 1:금액, 2:수량
    var trde_tp: String? = null, // 매매구분 0:순매수, 1:매수, 2:매도
    var unit_tp: String? = null // 단위구분 1000:천주, 1:단주
)