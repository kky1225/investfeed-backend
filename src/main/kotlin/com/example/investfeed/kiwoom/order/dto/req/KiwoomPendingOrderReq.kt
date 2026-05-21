package com.example.investfeed.kiwoom.order.dto.req

/** 미체결요청(ka10075). */
data class KiwoomPendingOrderReq(
    var all_stk_tp: String = "0", // 전체종목구분 (0:전체, 1:종목)
    var trde_tp: String = "0",    // 매매구분 (0:전체, 1:매도, 2:매수)
    var stk_cd: String? = null,   // 종목코드 (all_stk_tp=1 시)
    var stex_tp: String = "1",    // 거래소구분 (0:통합, 1:KRX, 2:NXT) — 모의는 KRX
)