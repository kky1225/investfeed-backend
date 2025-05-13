package com.example.investfeed.kiwoom.investor.dto.res

data class InvestorTradeRankRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var orgn_frgnr_cont_trde_prst: List<InvestorTradeRank> // 기관외국인연속매매현황
)