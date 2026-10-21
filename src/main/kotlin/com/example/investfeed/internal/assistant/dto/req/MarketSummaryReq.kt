package com.example.investfeed.internal.assistant.dto.req

import com.example.investfeed.internal.assistant.tool.DataToolService

data class MarketSummaryReq(
    val market: DataToolService.Market? = null, // 미지정 = 국내 지수 전체 (코스피·코스닥)
    val date: String? = null,                   // TODAY·YESTERDAY·YYYY-MM-DD (AssistantDate)
)
