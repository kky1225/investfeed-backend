package com.example.investfeed.domain.marketindex.dto.res

import java.time.LocalDateTime

data class MarketIndexRes(
    val type: String,
    val name: String,
    val price: String,
    val changeAmount: String,
    val changeRate: String,
    val delayStatus: String,
    val updatedAt: LocalDateTime,
)
