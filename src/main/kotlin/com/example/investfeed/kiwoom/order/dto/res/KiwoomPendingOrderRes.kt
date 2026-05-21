package com.example.investfeed.kiwoom.order.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

/** 미체결요청(ka10075) 응답. oso = 미체결 주문 리스트. */
data class KiwoomPendingOrderRes(
    override var return_code: Int,
    override var return_msg: String,
    var oso: List<KiwoomPendingOrder>? = null,
) : KiwoomRes(return_code, return_msg)

data class KiwoomPendingOrder(
    var acnt_no: String? = null,     // 계좌번호
    var ord_no: String? = null,      // 주문번호
    var stk_cd: String? = null,      // 종목코드
    var stk_nm: String? = null,      // 종목명
    var ord_stt: String? = null,     // 주문상태
    var ord_qty: String? = null,     // 주문수량
    var ord_pric: String? = null,    // 주문가격
    var oso_qty: String? = null,     // 미체결수량
    var orig_ord_no: String? = null, // 원주문번호
    var trde_tp: String? = null,     // 매매구분
    var stex_tp: String? = null,     // 거래소구분
)