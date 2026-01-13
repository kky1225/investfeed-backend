package com.example.investfeed.kiwoom.price.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomGoldPriceNowMinuteRes(
    override var return_code: Int, // 결과 코드
    override var return_msg: String, // 결과 메세지
    var gold_bid: List<KiwoomGoldPriceNowMinute>? = null // 금현물호가
): KiwoomRes(
    return_code = return_code,
    return_msg = return_msg
)