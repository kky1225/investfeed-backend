package com.example.investfeed.kiwoom.sect.dto.res

data class SectPriceRes(
    var return_code: Int,
    var return_msg: String,
    var inds_stkpc: List<SectPrice>? = null
)