package com.example.investfeed.kiwoom.realtime.dto

data class KiwoomGoldPriceStream(
    var item: List<String>? = null, // 실시간 등록 요소 거래소별 종목코드, 업종코드 (KRX:039490,NXT:039490_NX,SOR:039490_AL)
    var type: List<String> // 실시간 항목 TR 명(0A,0B....)
)