package com.example.investfeed.domain.monitoring.dto.req

import java.time.LocalDate

data class ErrorLogsReq(
    val acknowledged: Boolean? = null,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val messageKeyword: String? = null,
    val page: Int = 0,
    val size: Int = 50,
)
