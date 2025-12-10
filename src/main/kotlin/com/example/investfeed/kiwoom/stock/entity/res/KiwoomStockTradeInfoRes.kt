package com.example.investfeed.kiwoom.stock.entity.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomStockTradeInfoRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var date: String? = null, // 날짜
    var open_pric: String? = null, // 시가
    var high_pric: String? = null, // 고가
    var low_pric: String? = null, // 저가
    var close_pric: String? = null, // 종가
    var pre: String? = null, // 대비
    var flu_rt: String? = null, // 등락률
    var trde_qty: String? = null, // 거래량
    var trde_prica: String? = null, // 거래대금
    var cntr_str: String? = null, // 체결강도
): KiwoomRes(return_code, return_msg)