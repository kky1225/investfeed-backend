package com.example.investfeed.kiwoom.stock.dto.res

data class KiwoomStockChartDayRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var stk_cd: String? = null, // 종목코드
    var stk_dt_pole_chart_qry: List<KiwoomStockChartDay>? = null // 주식일봉차트조회
)