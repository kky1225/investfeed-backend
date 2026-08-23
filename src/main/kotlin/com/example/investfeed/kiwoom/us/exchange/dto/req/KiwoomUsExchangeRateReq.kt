package com.example.investfeed.kiwoom.us.exchange.dto.req

data class KiwoomUsExchangeRateReq(
    var exch_tp: String, // 환전구분 1:원화(KRW)->달러(USD), 2:달러(USD)->원화(KRW)
)