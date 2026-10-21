package com.example.investfeed.internal.assistant.dto.req

data class NewsSearchReq(
    val query: String,
    val n: Int = 5,
)
