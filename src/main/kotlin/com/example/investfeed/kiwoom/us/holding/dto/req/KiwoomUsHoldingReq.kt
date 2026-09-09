package com.example.investfeed.kiwoom.us.holding.dto.req

data class KiwoomUsHoldingReq(
    var stex_tp: String = "", // 거래소구분 ND:NASDAQ, NY:NYSE, NA:AMEX (미입력시 전체)
    var stk_cd: String = "", // 종목코드 (미입력시 전체)
)
