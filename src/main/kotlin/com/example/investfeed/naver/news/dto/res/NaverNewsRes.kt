package com.example.investfeed.naver.news.dto.res

data class NaverNewsRes(
    val lastBuildDate: String? = null,
    val total: Int? = null,
    val start: Int? = null,
    val display: Int? = null,
    val items: List<NaverNewsItem>? = null
)

data class NaverNewsItem(
    val title: String? = null,
    val originallink: String? = null,
    val link: String? = null,
    val description: String? = null,
    val pubDate: String? = null
)
