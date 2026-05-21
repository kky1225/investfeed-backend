package com.example.investfeed.kiwoom.order.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

/** 취소주문(kt10003) 응답. */
data class KiwoomCancelOrderRes(
    override var return_code: Int,
    override var return_msg: String,
    var ord_no: String? = null,           // 주문번호(취소주문)
    var base_orig_ord_no: String? = null, // 모주문번호
    var cncl_qty: String? = null,         // 취소수량
) : KiwoomRes(return_code, return_msg)