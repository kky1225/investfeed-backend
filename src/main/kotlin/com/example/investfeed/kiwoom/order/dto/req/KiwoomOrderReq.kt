package com.example.investfeed.kiwoom.order.dto.req

/**
 * 주식 매수(kt10000)/매도(kt10001) 주문 요청 — 두 API 요청 구조 동일.
 * 시장가 주문 시 trde_tp="3", ord_uv="", cond_uv="".
 */
data class KiwoomOrderReq(
    var dmst_stex_tp: String,      // 국내거래소구분 KRX,NXT,SOR (모의는 KRX)
    var stk_cd: String,            // 종목코드
    var ord_qty: String,           // 주문수량
    var ord_uv: String = "",       // 주문단가 (시장가 시 "")
    var trde_tp: String = "3",     // 매매구분 (3:시장가)
    var cond_uv: String = "",      // 조건단가
)