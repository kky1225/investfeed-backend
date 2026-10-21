package com.example.investfeed.internal.assistant.dto.req

import com.example.investfeed.internal.assistant.tool.DataToolService
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

data class CalendarToolReq(
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val from: LocalDate,
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val to: LocalDate,
    val keyword: String? = null,
    val type: DataToolService.CalendarType? = null,
    val country: DataToolService.CalendarCountry? = null,
)
