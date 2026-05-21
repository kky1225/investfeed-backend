package com.example.investfeed.domain.rebalancing.dto.res

data class AssetRatioStatus(
    val stockRatio: Double,
    val cryptoRatio: Double,
    val cashRatio: Double,
    val stockAmount: Long,
    val cryptoAmount: Long,
    val cashAmount: Long,
    val totalAsset: Long
)