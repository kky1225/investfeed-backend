package com.example.investfeed.kiwoom.chart.dto.stock.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomStockChartMinuteRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var stk_cd: String? = null, // 종목코드
    var stk_min_pole_chart_qry: List<KiwoomStockChartMinute>? = null // 주식분봉차트조회
): KiwoomRes(return_code, return_msg)