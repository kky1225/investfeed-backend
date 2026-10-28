package com.example.investfeed.domain.cryptowatchlist.dto.res

data class CryptoWatchlistItemRes(
    val id: Long,
    val market: String,
    val koreanName: String,
    var tradePrice: Double? = null,
    var signedChangeRate: Double? = null,
    var change: String? = null,
)
