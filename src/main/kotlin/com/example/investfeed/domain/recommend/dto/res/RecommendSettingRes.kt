package com.example.investfeed.domain.recommend.dto.res

import com.example.investfeed.domain.recommend.entity.RiskPreset

data class RecommendSettingRes(
    val riskPreset: RiskPreset,
    val priceVolatilityEnabled: Boolean,
    val movingAverageEnabled: Boolean,
    val marketIndexEnabled: Boolean,
    val volumePriceEnabled: Boolean,
    val rsiEnabled: Boolean,
    val highLow52wEnabled: Boolean,
    val breakoutEnabled: Boolean,
)