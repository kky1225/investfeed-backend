package com.example.investfeed.kiwoom.chart.dto.sect.res

data class SectChartDayList(
    var cur_prc: String, // 현재가
    var trde_qty: String, // 거래량
    var dt: String, // 일자
    var open_pric: String, // 시가
    var high_pric: String, // 고가
    var low_pric: String, // 저가
    var trde_prica: String, // 거래대금
    var bic_inds_tp: String, // 대업종구분
    var sm_inds_tp: String, // 소업종구분
    var stk_infr: String, // 종목정보
    var pred_close_pric: String // 전일종가
)