package com.example.investfeed.kiwoom.order.dto.req

/** 위탁종합거래내역 요청 (kt00015). 국내 주식의 매매만 받도록 기본값을 맞춰 둔다. */
data class KiwoomTradeHistoryReq(
    var strt_dt: String,                // YYYYMMDD
    var end_dt: String,                 // YYYYMMDD
    var stk_cd: String,                 // 종목코드 6자리 (앞의 A 는 붙여도 떼어도 같다. 2026-11-18 실측)
    var tp: String = "3",               // 3:매매
    var crnc_cd: String = "",
    var gds_tp: String = "1",           // 1:국내주식
    var frgn_stex_code: String = "",
    var dmst_stex_tp: String = "%",     // %:전체 (KRX·NXT)
    var qry_sort_tp: String = "2",      // 2:과거거래순
)
