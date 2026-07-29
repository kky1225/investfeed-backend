package com.example.investfeed.kiwoom.us.stock.dto.req

data class KiwoomUsStockInfoReq(
    var stex_tp: String, // 거래소구분 NA:AMEX, ND:NASDAQ, NY:NYSE
    var stk_cd: String, // 종목코드 (티커)
)
