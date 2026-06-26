package com.example.investfeed.toss.exchangerate.dto.res

data class TossExchangeRateRes(
    var result: TossExchangeRate? = null
)

data class TossExchangeRate(
    var baseCurrency: String? = null, // 기준 통화 (USD)
    var quoteCurrency: String? = null, // 상대 통화 (KRW)
    var rate: String? = null           // 환율 (1 baseCurrency = rate quoteCurrency)
)
