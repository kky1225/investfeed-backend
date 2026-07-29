package com.example.investfeed.kiwoom.us.stock.dto.req

data class KiwoomUsStockInfoListReq(
    var stex_tp: String, // 거래소구분 %:전체, NA:AMEX, ND:NASDAQ, NY:NYSE
)
