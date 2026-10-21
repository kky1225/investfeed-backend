package com.example.investfeed.internal.assistant.dto.req

import com.example.investfeed.internal.assistant.tool.StockMarket

data class StockQuoteReq(
    val stockQuery: List<String>,
    val market: StockMarket? = null,
    val date: String? = null,   // 미지정 = 현재 시세. 미국은 현지 날짜
)
