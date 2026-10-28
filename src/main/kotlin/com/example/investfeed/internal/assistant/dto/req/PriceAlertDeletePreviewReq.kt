package com.example.investfeed.internal.assistant.dto.req

data class PriceAlertDeletePreviewReq(
    val stockQuery: String,
    val price: Double? = null,   // 말했을 때만 — 그 목표가의 알림만 (국내·코인 원, 미국 달러)
)
