package com.example.investfeed.domain.monitoring.dto.res

data class SystemOverviewRes(
    val system: SystemStatusRes,
    val unackCount: UnacknowledgedCountRes,
)
