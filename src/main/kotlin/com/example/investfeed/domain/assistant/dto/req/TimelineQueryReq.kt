package com.example.investfeed.domain.assistant.dto.req

import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

data class TimelineQueryReq(
    val before: Long? = null,
    val limit: Int = 20,
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val date: LocalDate? = null,
)
