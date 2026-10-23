package com.example.investfeed.pressrelease.dto.res

import java.time.ZonedDateTime

data class PressReleaseItem(
    val title: String,
    val link: String,
    val description: String,   // 태그·엔티티를 걷어낸 본문 텍스트
    val publishedAt: ZonedDateTime,
)
