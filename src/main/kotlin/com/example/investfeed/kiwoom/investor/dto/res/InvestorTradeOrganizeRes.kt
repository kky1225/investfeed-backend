package com.example.investfeed.kiwoom.investor.dto.res

data class InvestorTradeOrganizeRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var stk_invsr_orgn_tot: List<InvestorTradeOrganize>? = null // 종목별투자자기관별합계
)