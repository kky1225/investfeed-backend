package com.example.investfeed.kiwoom.stockinfo.dto.req

data class StockInfoDailyTradeReq (
    var stk_cd: String, // KRX:039490,NXT:039490_NX,SOR:039490_AL
    var strt_dt: String // YYYYMMDD
)