package com.example.investfeed.kiwoom.chart.dto.stock.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomStockChartInvestorRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var opmr_invsr_trde_chart: List<KiwoomStockChartInvestor>? = null // 장중투자자별매매차트
): KiwoomRes(return_code = return_code, return_msg = return_msg)