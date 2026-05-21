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
)