package com.example.investfeed.domain.crypto.dto.res

data class CryptoChart(
    var dt: String? = null,
    var tradePrice: Double? = null,
    var openingPrice: Double? = null,
    var highPrice: Double? = null,
    var lowPrice: Double? = null,
    var candleAccTradeVolume: Double? = null,
    var candleAccTradePrice: Double? = null,
)
