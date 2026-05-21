package com.example.investfeed.domain.calendar.dto.req

import jakarta.validation.constraints.NotBlank

data class IndicatorHistoryReq(
    @field:NotBlank
    val code: String,
    @field:NotBlank
    val country: String // KR / US
)