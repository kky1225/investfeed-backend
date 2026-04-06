리package com.example.investfeed.kiwoom.holding.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomDepositRes(
    override var return_code: Int,
    override var return_msg: String,
    var entr: String? = null, // 예수금
    var ord_alow_amt: String? = null, // 주문가능금액
    var pymn_alow_amt: String? = null, // 출금가능금액
): KiwoomRes(return_code, return_msg)
