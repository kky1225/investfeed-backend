package com.example.investfeed.domain.calendar.dto.res

data class IndicatorDataPoint(
    val date: String,
    val value: String,
    val originalValue: String? = null,
    val observationDate: String? = null, // 관측월 (YYYY-MM-DD) — date 는 발표일
)