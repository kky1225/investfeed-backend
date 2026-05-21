package com.example.investfeed.kiwoom.order.dto.req

/**
 * 계좌별 주문체결내역 상세 요청 (kt00007).
 * 모의투자 도메인 지원 (KRX 만). qry_tp=4 = 체결내역만.
 */
data class KiwoomTradeFillsReq(
    var ord_dt: String,                    // YYYYMMDD (빈 값=전체)
    var qry_tp: String = "4",              // 4:체결내역만
    var stk_bond_tp: String = "0",         // 0:전체, 1:주식, 2:채권
    var sell_tp: String = "0",             // 0:전체, 1:매도, 2:매수
    var stk_cd: String = "",               // 공백허용
    var fr_ord_no: String = "",            // 공백허용
    var dmst_stex_tp: String = "KRX",      // %:전체, KRX, NXT, SOR
)