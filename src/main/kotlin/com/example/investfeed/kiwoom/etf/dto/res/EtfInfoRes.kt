package com.example.investfeed.kiwoom.etf.dto.res

data class EtfInfoRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var stk_nm: String? = null, // 종목명
    var etfobjt_idex_nm: String? = null, // ETF 대상지수명
    var wonju_pric: String? = null, // 원주가격
    var etftxon_type: String? = null, // ETF 과세유형
    var etntxon_type: String? = null // ETN 과세유형
)