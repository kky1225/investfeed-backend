package com.example.investfeed.internal.assistant.dto.req

import com.example.investfeed.domain.notification.entity.PriceTargetDirection

data class PriceAlertPreviewReq(
    val stockQuery: String,
    val price: Long,
    val direction: PriceTargetDirection,
)
