package com.example.investfeed.kiwoom.chart.dto.req

data class ChartYearListReq(
    var stk_cd: String, // 종목코드 거래소별 종목코드(KRX:039490,NXT:039490_NX,SOR:039490_AL)
    var base_dt: String, // 기준일자 YYYYMMDD
    var upd_stkpc_tp: String // 수정주가구분 0 or 1
)