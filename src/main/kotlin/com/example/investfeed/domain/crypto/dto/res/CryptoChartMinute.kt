package com.example.investfeed.domain.crypto.dto.res

/** 목록 화면 카드의 분봉 한 개. 시가·고가·저가는 카드를 캔들로 그릴 때 쓴다 */
data class CryptoChartMinute(
    var tradePrice: Double? = null,
    var candleDateTimeKst: String? = null,
    var openingPrice: Double? = null,
    var highPrice: Double? = null,
    var lowPrice: Double? = null,
)
