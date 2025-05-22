package com.example.investfeed.kiwoom.stock.dto.req

data class StockTradeDailyListReq (
    var stk_cd: String, // KRX:039490,NXT:039490_NX,SOR:039490_AL
    var strt_dt: String // YYYYMMDD
)