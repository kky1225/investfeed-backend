package com.example.investfeed.kiwoom.stock.entity.res

data class KiwoomStockChartYear(
    var cur_prc: String? = null, // 현재가
    var trde_qty: String? = null, // 거래량
    var trde_prica: String? = null, // 거래대금
    var dt: String? = null, // 일자
    var open_pric: String? = null, // 시가
    var high_pric: String? = null, // 고가
    var low_pric: String? = null, // 저가
    var upd_stkpc_tp: String? = null, // 수정주가구분 1:유상증자, 2:무상증자, 4:배당락, 8:액면분할, 16:액면병합, 32:기업합병, 64:감자, 256:권리
    var upd_rt: String? = null, // 수정비율
    var bic_inds_tp: String? = null, // 대업종구분
    var sm_inds_tp: String? = null, // 소업종구분
    var stk_infr: String? = null, // 종목정보
    var upd_stkpc_event: String? = null, // 수정주가이벤트
    var pred_close_pric: String? = null // 전일종가
)