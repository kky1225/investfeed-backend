package com.example.investfeed.kiwoom.rank.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomInvestorTradeRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var frgnr_orgn_trde_upper: List<KiwoomInvestorTrade>? = null // 외국인기관매매상위
): KiwoomRes(return_code, return_msg)