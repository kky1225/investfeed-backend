package com.example.investfeed.kiwoom.price.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomInvestorTradeCloseMarketRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var opaf_invsr_trde: List<KiwoomInvestorTradeCloseMarketItemList>? = null // 장마감후투자자별매매
): KiwoomRes(return_code, return_msg)