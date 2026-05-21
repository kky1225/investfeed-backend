package com.example.investfeed.kiwoom.order.dto.req

/** 주식 취소주문(kt10003) 요청. cncl_qty="0" 이면 잔량 전부 취소. */
data class KiwoomCancelOrderReq(
    var dmst_stex_tp: String,   // 국내거래소구분 (모의는 KRX)
    var orig_ord_no: String,    // 원주문번호
    var stk_cd: String,         // 종목코드
    var cncl_qty: String = "0", // 취소수량 ("0"=잔량 전부 취소)
)