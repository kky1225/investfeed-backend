package com.example.investfeed.kiwoom.stock.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomStockTradeDailyRes (
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var daly_trde_dtl: List<KiwoomStockTradeDaily>? = null // 일별거래상세
): KiwoomRes(return_code, return_msg)