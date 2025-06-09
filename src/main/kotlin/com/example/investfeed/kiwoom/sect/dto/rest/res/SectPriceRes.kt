package com.example.investfeed.kiwoom.sect.dto.rest.res

data class SectPriceRes(
    var return_code: Int, // 결과 코드
    var return_msg: String, // 결과 메세지
    var inds_stkpc: List<SectPrice>? = null // 업종별주가
)