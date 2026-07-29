package com.example.investfeed.kiwoom.us.rank.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomUsStockTradeValueListRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var result_list: List<KiwoomUsStockTradeValueRes>? = null // 당일 거래대금 상위
): KiwoomRes(return_code, return_msg)
