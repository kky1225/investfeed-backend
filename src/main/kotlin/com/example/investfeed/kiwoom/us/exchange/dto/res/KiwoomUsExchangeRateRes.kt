package com.example.investfeed.kiwoom.us.exchange.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomUsExchangeRateRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var sell_aplc_exrt: String? = null, // 매도적용환율
    var buy_aplc_exrt: String? = null, // 매수적용환율
    var aplc_exrt: String? = null, // 적용환율 (실제 환전에 적용되는 환율)
    var exrt_tp_nm: String? = null, // 환율구분명 (예: 고시환율)
    var spcl_bf_exrt: String? = null, // 우대율 적용 전 환율
    var exrt_spcl_rt: String? = null, // 환율우대율
): KiwoomRes(return_code, return_msg)
