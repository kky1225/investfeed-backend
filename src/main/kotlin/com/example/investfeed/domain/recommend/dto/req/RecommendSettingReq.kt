package com.example.investfeed.domain.recommend.dto.req

import com.example.investfeed.domain.recommend.entity.RiskPreset

data class RecommendSettingReq(
    val riskPreset: RiskPreset,
)