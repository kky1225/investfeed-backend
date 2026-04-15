package com.example.investfeed.fred.dto.res

data class FredReleaseDatesRes(
    val realtime_start: String? = null,
    val realtime_end: String? = null,
    val order_by: String? = null,
    val sort_order: String? = null,
    val count: Int? = null,
    val offset: Int? = null,
    val limit: Int? = null,
    val release_dates: List<FredReleaseDate>? = null
)

data class FredReleaseDate(
    val release_id: Int? = null,
    val release_name: String? = null,
    val date: String? = null,
)
