package com.example.investfeed.kiwoom.us.chart.dto.req

data class KiwoomUsStockChartReq(
    var stex_tp: String, // 거래소구분 NA:AMEX, ND:NASDAQ, NY:NYSE
    var stk_cd: String, // 종목코드 (티커)
    var tic_scope: String? = null, // 틱범위 (분봉 전용: 1, 3, 5, 10, 30)
)
