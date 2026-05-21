package com.example.investfeed.domain.monitoring.dto.res

import org.springframework.data.domain.Page

data class ConfigLogsOverviewRes(
    val logs: Page<SchedulerConfigLogRes>,
    val unackCount: UnacknowledgedCountRes,
)