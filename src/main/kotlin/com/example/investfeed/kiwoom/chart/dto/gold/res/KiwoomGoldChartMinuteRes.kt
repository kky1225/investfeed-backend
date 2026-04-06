package com.example.investfeed.kiwoom.chart.dto.gold.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomGoldChartMinuteRes (
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var gds_min_chart_qry: List<KiwoomGoldChartMinute>? = null // 금현물분봉차트조회
): KiwoomRes(return_code, return_msg)