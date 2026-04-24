package com.example.investfeed.domain.marketindex.dto.res

import com.example.investfeed.domain.crypto.dto.res.FearGreedRes

data class MarketIndexDashboardRes(
    val indices: List<MarketIndexRes>,
    val fearGreed: FearGreedRes? = null,
    val bitcoin: BitcoinSummary? = null,
    val ethereum: BitcoinSummary? = null,
)

data class BitcoinSummary(
    val price: String,
    val changeAmount: String,
    val changeRate: String,
    val trend: String, // UP, DOWN, EVEN
)
