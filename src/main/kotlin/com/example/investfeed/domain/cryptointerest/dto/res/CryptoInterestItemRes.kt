package com.example.investfeed.domain.cryptointerest.dto.res

data class CryptoInterestItemRes(
    val id: Long,
    val market: String,
    val koreanName: String,
    var tradePrice: Double? = null,
    var signedChangeRate: Double? = null,
    var change: String? = null,
)
