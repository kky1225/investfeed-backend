package com.example.investfeed.kiwoom.us.stock.dto.req

data class KiwoomUsStockStreamItem(
    var jmcode: String, // 종목코드 (티커)
    var stex_tp: String, // 거래소구분 ND:NASDAQ, NY:NYSE, NA:AMEX
)
