package com.example.investfeed.internal.assistant.dto.req

data class ExchangeRateReq(
    val date: String? = null, // 미지정 = 지금(실시간 고시회차). 지정 = 그날 최종 고시
)
