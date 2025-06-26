package com.example.investfeed.kiwoom.chart.dto.sect.res

data class SectChartYearList(
    var cur_prc: String? = null, // 현재가
    var trde_qty: String? = null, // 거래량
    var dt: String? = null, // 일자
    var open_pric: String? = null, // 시가
    var high_pric: String? = null, // 고가
    var low_pric: String? = null, // 저가
    var trde_prica: String? = null, // 거래대금
    var bic_inds_tp: String? = null, // 대업종구분
    var sm_inds_tp: String? = null, // 소업종구분
    var stk_infr: String? = null, // 종목정보
    var pred_close_pric: String? = null, // 전일종가
)