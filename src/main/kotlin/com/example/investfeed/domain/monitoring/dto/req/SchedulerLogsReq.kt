package com.example.investfeed.domain.monitoring.dto.req

data class SchedulerLogsReq(
    val schedulerName: String? = null,
    val status: String? = null,
    val page: Int = 0,
    val size: Int = 50,
)
