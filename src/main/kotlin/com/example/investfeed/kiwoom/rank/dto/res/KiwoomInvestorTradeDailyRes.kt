package com.example.investfeed.kiwoom.rank.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomInvestorTradeDailyRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var opmr_invsr_trde_upper: List<KiwoomInvestorTradeDaily>? = null // 장중투자자별매매상위
): KiwoomRes(return_code, return_msg)