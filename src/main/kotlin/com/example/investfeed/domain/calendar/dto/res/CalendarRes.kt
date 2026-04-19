package com.example.investfeed.domain.calendar.dto.res

data class EconomicIndicatorsRes(
    val indicators: List<EconomicIndicator>,
    val lastUpdated: String? = null
)

data class EconomicIndicator(
    val code: String,
    val name: String,
    val country: String,
    val latestValue: String,
    val latestDate: String,
    val unit: String,
    val change: String?,
    val previousValue: String? = null, // "이전 발표값" 표시용 (PAYEMS 등 증감 기준 지표)
)

data class IndicatorHistoryRes(
    val code: String,
    val name: String,
    val unit: String,
    val chartType: String = "linear", // linear, stepAfter
    val frequency: String = "D",      // D, M, Q
    val data: List<IndicatorDataPoint>
)

data class IndicatorDataPoint(
    val date: String,
    val value: String,
    val originalValue: String? = null,
    val observationDate: String? = null, // 관측월 (YYYY-MM-DD) — date 는 발표일
)

data class CalendarEventsRes(
    val events: List<CalendarEvent>,
    val lastUpdated: String? = null
)

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
