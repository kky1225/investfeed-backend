package com.example.investfeed.kiwoom.investor.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class InvestorTradeRankListRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var orgn_frgnr_cont_trde_prst: List<InvestorTradeRankList>? = null // 기관외국인연속매매현황
): KiwoomRes(return_code, return_msg)