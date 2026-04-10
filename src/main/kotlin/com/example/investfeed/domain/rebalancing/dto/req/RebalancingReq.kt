package com.example.investfeed.domain.rebalancing.dto.req

data class RebalancingSettingReq(
    val stockRatio: Int,
    val stockDirection: String, // MIN 또는 MAX
    val cryptoRatio: Int,
    val cryptoDirection: String,
    val cashRatio: Int,
    val cashDirection: String,
    val maxStockRatio: Int
)
