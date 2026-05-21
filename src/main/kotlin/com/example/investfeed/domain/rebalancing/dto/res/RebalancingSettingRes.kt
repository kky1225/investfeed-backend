package com.example.investfeed.domain.rebalancing.dto.res

data class RebalancingSettingRes(
    val stockRatio: Int,
    val stockDirection: String,
    val cryptoRatio: Int,
    val cryptoDirection: String,
    val cashRatio: Int,
    val cashDirection: String,
    val maxStockRatio: Int
)