package com.example.investfeed.domain.calendar.dto.res

data class CalendarEvent(
    val id: Long? = null,
    val date: String,
    val name: String,
    val country: String,
    val value: String?,
    val isFuture: Boolean,
    val type: String = "INDICATOR", // INDICATOR, HOLIDAY, MEETING
    val source: String = "ECOS",   // ECOS, FRED, HOLIDAY, MANUAL
    val note: String? = null,      // 값 옆에 보일 안내. 값이 대체 지표면 "전월비 · 전년비 미발표", 값이 없으면 "미발표"
)