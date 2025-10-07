package com.example.investfeed.kiwoom.chart.dto.gold.res

data class GoldChartMonthListRes (
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var gds_month_chart_qry: List<GoldChartDayList>? = null // 금현물월봉차트조회
)