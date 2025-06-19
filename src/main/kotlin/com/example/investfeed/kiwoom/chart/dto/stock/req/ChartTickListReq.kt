package com.example.investfeed.kiwoom.chart.dto.stock.req

data class ChartTickListReq(
    var stk_cd: String, // 종목코드 거래소별 종목코드(KRX:039490,NXT:039490_NX,SOR:039490_AL)
    var tic_scope: String, // 틱범위 1:1틱, 3:3틱, 5:5틱, 10:10틱, 30:30틱
    var upd_stkpc_tp: String // 수정주가구분 0 or 1
)