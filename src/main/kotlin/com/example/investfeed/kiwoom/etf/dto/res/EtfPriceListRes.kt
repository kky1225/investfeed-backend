package com.example.investfeed.kiwoom.etf.dto.res

data class EtfPriceListRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var etfall_mrpr: List<EtfPriceList>? = null // ETF 전체시세
)