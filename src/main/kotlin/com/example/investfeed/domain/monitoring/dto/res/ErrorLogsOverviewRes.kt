package com.example.investfeed.domain.monitoring.dto.res

import org.springframework.data.domain.Page

data class ErrorLogsOverviewRes(
    val logs: Page<ErrorLogRes>,
    val unackCount: UnacknowledgedCountRes,
)