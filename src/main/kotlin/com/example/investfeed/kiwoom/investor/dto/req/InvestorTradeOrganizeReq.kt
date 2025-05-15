package com.example.investfeed.kiwoom.investor.dto.req

data class InvestorTradeOrganizeReq(
    var stk_cd: String, // 종목코드 KRX:039490,NXT:039490_NX,SOR:039490_AL
    var strt_dt: String, // 시작일자 YYYYMMDD
    var end_dt: String, // 종료일자 YYYYMMDD
    var amt_qty_tp: String, // 금액수량구분 1:금액, 2:수량
    var trde_tp: String, // 매매구분 0:순매수, 1:매수, 2:매도
    var unit_tp: String // 단위구분 1000:천주, 1:단주
)