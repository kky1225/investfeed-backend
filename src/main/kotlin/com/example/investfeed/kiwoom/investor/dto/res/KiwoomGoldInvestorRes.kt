package com.example.investfeed.kiwoom.investor.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomGoldInvestorRes(
    override var return_code: Int, // 결과 코드
    override var return_msg: String, // 결과 메세지
    var inve_trad_stat: List<KiwoomGoldInvestor>? = null //금현물투자자현황
): KiwoomRes(return_code, return_msg)