package com.example.investfeed.domain.us.rank.dto.req

data class UsStockStreamItem(
    var stkCd: String, // 종목코드 (티커)
    var stexTp: String, // 거래소구분 ND:NASDAQ, NY:NYSE, NA:AMEX
)
