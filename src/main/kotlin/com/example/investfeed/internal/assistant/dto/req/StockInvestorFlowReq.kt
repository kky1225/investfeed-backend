package com.example.investfeed.internal.assistant.dto.req

data class StockInvestorFlowReq(
    val stockQuery: List<String>,
    val days: Int = 5,
    val date: String? = null,   // 미지정 = 오늘까지
)
