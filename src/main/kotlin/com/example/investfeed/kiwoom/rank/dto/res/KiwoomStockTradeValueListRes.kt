package com.example.investfeed.kiwoom.rank.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomStockTradeValueListRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var trde_prica_upper: List<KiwoomStockTradeValueRes>? = null // 거래대금상위
): KiwoomRes(return_code, return_msg)