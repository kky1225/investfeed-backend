package com.example.investfeed.kiwoom.chart.dto.gold.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomGoldChartMonthRes (
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var gds_month_chart_qry: List<KiwoomGoldChartMonth>? = null // 금현물월봉차트조회
): KiwoomRes(return_code, return_msg)