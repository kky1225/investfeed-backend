package com.example.investfeed.domain.news.dto.req

data class NewsSearchReq(
    val query: String,
    val page: Int = 1
)
