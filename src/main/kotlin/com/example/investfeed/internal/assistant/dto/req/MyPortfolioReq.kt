package com.example.investfeed.internal.assistant.dto.req

import com.example.investfeed.internal.assistant.tool.DisplayToolService

data class MyPortfolioReq(
    val assetClass: DisplayToolService.AssetClass? = null,
    val dayRateLt: Double? = null,
    val dayRateGt: Double? = null,
    val totalRateLt: Double? = null,
    val totalRateGt: Double? = null,
)
