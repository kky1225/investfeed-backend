package com.example.investfeed.kiwoom.price.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomGoldPriceNowRes (
    override var return_code: Int, // 결과 코드
    override var return_msg: String, // 결과 메세지
    var pred_pre_sig: String? = null, // 전일대비기호
    var pred_pre: String? = null, // 전일대비
    var flu_rt: String? = null, // 등락율
    var trde_qty: String? = null, // 거래량
    var open_pric: String? = null, // 시가
    var high_pric: String? = null, // 고가
    var low_pric: String? = null, // 저가
    var pred_rt: String? = null, // 전일비
    var upl_pric: String? = null, // 상한가
    var lst_pric: String? = null, // 하한가
    var pred_close_pric: String? = null // 전일종가
): KiwoomRes(return_code, return_msg)