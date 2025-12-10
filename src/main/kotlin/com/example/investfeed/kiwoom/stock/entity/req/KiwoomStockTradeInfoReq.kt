package com.example.investfeed.kiwoom.stock.entity.req

data class KiwoomStockTradeInfoReq (
    var stk_cd: String, // 거래소별 종목코드(KRX:039490,NXT:039490_NX,SOR:039490_AL)
)