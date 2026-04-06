package com.example.investfeed.kiwoom.chart.dto.gold.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomGoldChartDayRes (
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var gds_day_chart_qry: List<KiwoomGoldChartDay>? = null // 금현물일봉차트조회
): KiwoomRes(return_code, return_msg)