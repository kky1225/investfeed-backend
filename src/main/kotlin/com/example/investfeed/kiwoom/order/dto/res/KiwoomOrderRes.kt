package com.example.investfeed.kiwoom.order.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

/** 매수(kt10000)/매도(kt10001) 주문 응답. */
data class KiwoomOrderRes(
    override var return_code: Int,
    override var return_msg: String,
    var ord_no: String? = null,        // 주문번호
    var dmst_stex_tp: String? = null,  // 국내거래소구분
) : KiwoomRes(return_code, return_msg)