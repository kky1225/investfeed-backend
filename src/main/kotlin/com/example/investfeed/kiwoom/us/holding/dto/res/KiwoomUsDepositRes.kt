package com.example.investfeed.kiwoom.us.holding.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomUsDepositRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var krw_entra: String? = null, // 원화예수금
    var result_list: List<KiwoomUsDepositItemRes>? = null, // 통화별 예수금
): KiwoomRes(return_code, return_msg)

data class KiwoomUsDepositItemRes(
    var crnc_code: String? = null, // 통화코드 (USD, GBP ...)
    var crnc_nm: String? = null, // 통화명
    var fc_entra: String? = null, // 외화예수금
    var fc_pymn_alowa: String? = null, // 외화출금가능금액
    var fc_booka: String? = null, // 외화장부금액(원화 환산)
    var fc_ord_alowa: String? = null, // 외화주문가능금액
)
