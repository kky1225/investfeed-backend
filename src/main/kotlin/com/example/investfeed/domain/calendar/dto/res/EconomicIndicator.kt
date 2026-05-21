package com.example.investfeed.domain.calendar.dto.res

data class EconomicIndicator(
    val code: String,
    val name: String,
    val country: String,
    val latestValue: String,
    val latestDate: String,
    val unit: String,
    val change: String?,
    val previousValue: String? = null, // "이전 발표값" 표시용 (PAYEMS 등 증감 기준 지표)
)