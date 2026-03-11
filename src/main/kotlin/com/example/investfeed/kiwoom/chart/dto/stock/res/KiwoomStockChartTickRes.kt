package com.example.investfeed.kiwoom.chart.dto.stock.res

data class KiwoomStockChartTickRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var stk_cd: String? = null, // 종목코드
    var last_tic_cnt: String? = null, // 마지막틱갯수
    var stk_tic_chart_qry: List<KiwoomStockChartTick>? = null, // 주식틱차트조회
)