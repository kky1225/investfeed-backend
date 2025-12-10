package com.example.investfeed.kiwoom.stock.entity.res

import com.example.investfeed.kiwoom.KiwoomRes

class KiwoomStockInvestorRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var stk_invsr_orgn: List<KiwoomStockInvestor>? = null, // 종목별투자자기관별
): KiwoomRes(return_code, return_msg)