package com.example.investfeed.kiwoom.investor.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class InvestorTradeOrganizeRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var stk_invsr_orgn_tot: List<InvestorTradeOrganize>? = null // 종목별투자자기관별합계
): KiwoomRes(return_code, return_msg)