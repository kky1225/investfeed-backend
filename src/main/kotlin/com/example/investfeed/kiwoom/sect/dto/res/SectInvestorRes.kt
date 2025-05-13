package com.example.investfeed.kiwoom.sect.dto.res

data class SectInvestorRes(
    var return_code: Int, // 결과 코드
    var return_msg: String, // 결과 메세지
    var inds_netprps: List<SectInvestor>? = null // 업종별순매수
)