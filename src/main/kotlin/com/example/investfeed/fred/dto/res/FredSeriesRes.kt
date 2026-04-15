package com.example.investfeed.fred.dto.res

data class FredSeriesRes(
    val realtime_start: String? = null,
    val realtime_end: String? = null,
    val observation_start: String? = null,
    val observation_end: String? = null,
    val units: String? = null,
    val output_type: Int? = null,
    val file_type: String? = null,
    val order_by: String? = null,
    val sort_order: String? = null,
    val count: Int? = null,
    val offset: Int? = null,
    val limit: Int? = null,
    val observations: List<FredObservation>? = null
)

data class FredObservation(
    val realtime_start: String? = null,
    val realtime_end: String? = null,
    val date: String? = null,
    val value: String? = null,
)
