package com.example.investfeed.domain.monitoring.dto.req

import java.time.LocalDate

data class SchedulerLogsReq(
    val schedulerName: String? = null,
    val status: String? = null,
    val acknowledged: Boolean? = null,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val messageKeyword: String? = null,
    val page: Int = 0,
    val size: Int = 50,
)
