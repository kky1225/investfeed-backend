package com.example.investfeed.domain.us.stock.dto.req

data class UsStockDetailStreamReq(
    val stkCd: String, // 종목코드 (티커)
    val stexTp: String, // 거래소구분 NA:AMEX, ND:NASDAQ, NY:NYSE
)