package com.example.investfeed.kiwoom.stock.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomStockInterestRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var atn_stk_infr: List<KiwoomStockInterest>? = null // 관심종목정보
): KiwoomRes(return_code = return_code ,return_msg = return_msg)