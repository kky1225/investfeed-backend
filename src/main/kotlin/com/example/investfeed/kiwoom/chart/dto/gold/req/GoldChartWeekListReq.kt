package com.example.investfeed.kiwoom.chart.dto.gold.req

data class GoldChartWeekListReq (
    var stk_cd: String, // 종목코드 M04020000 금 99.99_1kg, M04020100 미니금 99.99_100g
    var base_dt: String, // 기준일자 YYYYMMDD
    var upd_stkpc_tp: String? = null // 수정주가구분 0 or 1
)