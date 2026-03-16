package com.example.investfeed.kiwoom.stock.dto.req

data class KiwoomStockInterestReq(
    var stk_cd: String // 종목코드 거래소별 종목코드 (KRX:039490,NXT:039490_NX,SOR:039490_AL) 여러개의 종목코드 입력시 | 로 구분
)