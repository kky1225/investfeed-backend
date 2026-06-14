package com.example.investfeed.kiwoom.stock.dto.req

data class KiwoomStockViListReq(
    var mrkt_tp: String = "000",          // 시장구분 000:전체, 001:코스피, 101:코스닥
    var bf_mkrt_tp: String = "0",         // 장전구분 0:전체, 1:정규시장, 2:장외단일가
    var stk_cd: String,                   // 종목코드
    var motn_tp: String = "0",            // 발동구분 0:전체, 1:정적VI, 2:동적VI, 3:동적VI+정적VI
    var skip_stk: String = "000000000",   // 제외종목 (전종목포함)
    var trde_qty_tp: String = "0",        // 거래량구분 0:사용안함, 1:사용
    var min_trde_qty: String = "0",       // 최소거래량
    var max_trde_qty: String = "0",       // 최대거래량
    var trde_prica_tp: String = "0",      // 거래대금구분 0:사용안함, 1:사용
    var min_trde_prica: String = "0",     // 최소거래대금
    var max_trde_prica: String = "0",     // 최대거래대금
    var motn_drc: String = "0",           // 발동방향 0:전체, 1:상승, 2:하락
    var stex_tp: String = "3",            // 거래소구분 1:KRX, 2:NXT, 3:통합
)
