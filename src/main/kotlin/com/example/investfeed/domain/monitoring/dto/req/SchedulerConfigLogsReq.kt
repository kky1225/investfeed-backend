package com.example.investfeed.domain.monitoring.dto.req

data class SchedulerConfigLogsReq(
    val schedulerName: String? = null,
    val page: Int = 0,
    val size: Int = 50,
)
