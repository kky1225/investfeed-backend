package com.example.investfeed.domain.assistant.dto.res

data class TimelinePageRes(
    val items: List<TimelineMessageRes>,
    val nextCursor: Long?,
    val unreadCount: Long,
    val personalIncluded: Boolean,
)
