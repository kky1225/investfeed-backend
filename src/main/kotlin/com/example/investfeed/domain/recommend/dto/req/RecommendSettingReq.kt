package com.example.investfeed.domain.recommend.dto.req

import com.example.investfeed.domain.recommend.entity.RiskPreset

data class RecommendSettingReq(
    val riskPreset: RiskPreset,
    val priceVolatilityEnabled: Boolean = false,
    val movingAverageEnabled: Boolean = false,
    val marketIndexEnabled: Boolean = false,
    val volumePriceEnabled: Boolean = false,
    val rsiEnabled: Boolean = false,
    val highLow52wEnabled: Boolean = false,
)