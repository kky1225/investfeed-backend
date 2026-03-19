package com.example.investfeed.domain.crypto.dto.res

data class CryptoDetailInfo(
    var market: String? = null,
    var koreanName: String? = null,
    var englishName: String? = null,
    var tradePrice: Double? = null,
    var openingPrice: Double? = null,
    var highPrice: Double? = null,
    var lowPrice: Double? = null,
    var prevClosingPrice: Double? = null,
    var change: String? = null,
    var signedChangeRate: Double? = null,
    var signedChangePrice: Double? = null,
    var accTradePrice24h: Double? = null,
    var accTradeVolume24h: Double? = null,
    var highest52WeekPrice: Double? = null,
    var highest52WeekDate: String? = null,
    var lowest52WeekPrice: Double? = null,
    var lowest52WeekDate: String? = null,
    var tradeDateTimeKst: String? = null,
)
