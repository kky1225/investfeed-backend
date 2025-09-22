package com.example.investfeed.kiwoom.chart.dto.gold.res

data class GoldChartMinuteListRes (
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var gds_min_chart_qry: List<GoldChartMinuteList>? = null // 금현물분봉차트조회
)