package com.example.investfeed.kiwoom.chart.dto.gold.req

data class GoldChartMinuteListReq (
    var stk_cd: String, // 종목코드 M04020000 금 99.99_1kg, M04020100 미니금 99.99_100g
    var tic_scope: String, // 틱범위 1:1분, 3:3분, 5:5분, 10:10분, 15:15분, 30:30분, 45:45분, 60:60분
    var upd_stkpc_tp: String? = null // 수정주가구분 0 or 1
)