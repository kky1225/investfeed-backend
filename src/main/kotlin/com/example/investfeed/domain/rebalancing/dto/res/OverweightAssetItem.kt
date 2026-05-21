package com.example.investfeed.domain.rebalancing.dto.res

data class OverweightAssetItem(
    val assetType: String,
    val currentRatio: Double,
    val targetRatio: Int,
    val direction: String,
    val excessAmount: Long
)