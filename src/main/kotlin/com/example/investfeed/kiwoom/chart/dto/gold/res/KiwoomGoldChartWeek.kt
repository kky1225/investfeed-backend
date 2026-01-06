package com.example.investfeed.kiwoom.chart.dto.gold.res

data class KiwoomGoldChartWeek (
    var cur_prc: String? = null, // 현재가
    var acc_trde_qty: String? = null, // 누적거래량
    var acc_trde_prica: String? = null, // 누적 거래대금
    var open_pric: String? = null, // 시가
    var high_pric: String? = null, // 고가
    var low_pric: String? = null, // 저가
    var dt: String? = null, // 일자
)