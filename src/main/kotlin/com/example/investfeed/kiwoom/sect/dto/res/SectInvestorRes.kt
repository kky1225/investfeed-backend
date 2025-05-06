package com.example.investfeed.kiwoom.sect.dto.res

data class SectInvestorRes(
    var return_code: Int,
    var return_msg: String,
    var inds_netprps: List<SectInvestor>? = null
)