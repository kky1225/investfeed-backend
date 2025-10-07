package com.example.investfeed.kiwoom.gold.dto.rest.res

data class GoldInvestorRes(
    var return_code: Int, // 결과 코드
    var return_msg: String, // 결과 메세지
    var inve_trad_stat: List<GoldInvestor>? = null //금현물투자자현황
)