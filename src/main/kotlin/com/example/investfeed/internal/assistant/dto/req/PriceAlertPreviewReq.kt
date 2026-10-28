package com.example.investfeed.internal.assistant.dto.req

import com.example.investfeed.domain.notification.entity.PriceTargetDirection

data class PriceAlertPreviewReq(
    val stockQuery: String,
    val price: Double,   // 국내·코인 원, 미국 달러
    val direction: PriceTargetDirection,
)
