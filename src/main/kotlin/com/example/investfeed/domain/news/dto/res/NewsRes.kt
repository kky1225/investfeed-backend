package com.example.investfeed.domain.news.dto.res

data class NewsListRes(
    val items: List<NewsItem>,
    val total: Int
)

data class NewsItem(
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String,
    val sentiment: String? = null // 추후 AI 호재/악재 분석용 (POSITIVE/NEGATIVE/NEUTRAL)
)
