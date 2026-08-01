package com.example.investfeed.kiwoom.us.chart.dto.res

data class KiwoomUsStockChartItem(
    var dt: String? = null, // 일자 (일/주/월/년봉)
    var cntr_tm: String? = null, // 체결시각 yyyyMMddHHmmss (분봉)
    var bus_dt: String? = null, // 영업일자 (분봉)
    var cur_prc: String? = null, // 현재가(종가)
    var open_pric: String? = null, // 시가
    var high_pric: String? = null, // 고가
    var low_pric: String? = null, // 저가
    var acc_trde_qty: String? = null, // 누적거래량 (일/주/월/년봉)
    var trde_qty: String? = null, // 거래량 (분봉)
    var acc_trde_prica: String? = null, // 누적거래대금
    var pred_pre: String? = null, // 전일대비
    var flu_rt: String? = null, // 등락률
)
