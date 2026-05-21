package com.example.investfeed.domain.calendar.dto.res

data class CalendarEventsRes(
    val events: List<CalendarEvent>,
    val lastUpdated: String? = null
)