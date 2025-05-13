package com.example.investfeed.kiwoom.investor.dto.res

data class InvestorDailyTradeRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var invsr_daly_trde_stk: List<InvestorDailyTrade> // 투자자별일별매매종목
)