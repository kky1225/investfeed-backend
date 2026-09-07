package com.example.investfeed.domain.us.stock.dto.res

data class UsStockSearchItem(
    val stkCd: String, // 종목코드 (티커)
    val stkNm: String, // 종목명
    val stexTp: String, // 거래소구분 ND:NASDAQ, NY:NYSE, NA:AMEX
    val marketName: String, // 거래소명. stkNm 은 ETF 면 티커가 담긴다(UsEtfLookup 참고)
)