package com.example.investfeed.kiwoom.shortselling.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomStockShortSellingRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var shrts_trnsn: List<KiwoomStockShortSelling>? = null // 공매도추이
): KiwoomRes(return_code, return_msg)