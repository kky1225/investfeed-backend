package com.example.investfeed.kiwoom.chart.dto.stock.req

data class KiwoomStockChartMinuteReq(
    var stk_cd: String, // 종목코드 거래소별 종목코드(KRX:039490,NXT:039490_NX,SOR:039490_AL)
    var tic_scope: String, // 틱범위 1:1분, 3:3분, 5:5분, 10:10분, 15:15분, 30:30분, 45:45분, 60:60분
    var upd_stkpc_tp: String // 수정주가구분 0 or 1
)