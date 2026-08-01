package com.example.investfeed.kiwoom.us.chart.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomUsStockChartRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var result_list: List<KiwoomUsStockChartItem>? = null,
): KiwoomRes(return_code, return_msg)
