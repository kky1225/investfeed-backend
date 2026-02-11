package com.example.investfeed.kiwoom.price.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomInvestorTradeOpenMarketRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var opmr_invsr_trde: List<KiwoomInvestorTradeOpenMarketItemList>? = null // 장중투자자별매매
): KiwoomRes(return_code, return_msg)