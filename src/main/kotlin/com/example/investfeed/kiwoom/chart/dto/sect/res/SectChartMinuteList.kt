package com.example.investfeed.kiwoom.chart.dto.sect.res

data class SectChartMinuteList(
    var cur_prc: String? = null, // 현재가
    var trde_qty: String? = null, // 거래량
    var cntr_tm: String? = null, // 체결시간
    var open_pric: String? = null, // 시가
    var high_pric: String? = null, // 고가
    var low_pric: String? = null, // 저가
    var acc_trde_qty: String? = null, // 누적거래량
    var pred_pre: String? = null, // 전일대비
    var pred_pre_sig: String? = null, // 전일대비 기호
)