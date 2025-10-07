package com.example.investfeed.kiwoom.chart.dto.gold.res

data class GoldChartWeekListRes (
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var gds_week_chart_qry: List<GoldChartWeekList>? = null // 금현물일봉차트조회
)