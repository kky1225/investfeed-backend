package com.example.investfeed.kiwoom.chart.dto.stock.res

data class ChartYearListRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var stk_cd: String? = null, // 종목코드
    var stk_yr_pole_chart_qry: List<ChartYearList>? = null // 주식년봉차트조회
)