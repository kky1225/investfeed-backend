package com.example.investfeed.kiwoom.chart.dto.gold.res

data class GoldChartMinuteList (
    var cntr_pric: String? = null, // 체결가
    var pred_pre: String? = null, // 전일 대비(원)
    var acc_trde_qty: String? = null, // 누적거래량
    var acc_trde_prica: String? = null, // 거래대금
    var trde_qty: String? = null, // 거래량(체결량)
    var open_pric: String? = null, // 시가
    var high_pric: String? = null, // 고가
    var low_pric: String? = null, // 저가
    var cntr_tm: String? = null, // 체결시간
    var dt: String? = null, // 일자
    var pred_pre_sig: String? = null // 전일대비기호
)