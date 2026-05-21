package com.example.investfeed.domain.monitoring.dto.res

data class ApiCallsOverviewRes(
    val stats: ApiCallStatsRes,
    val unackCount: UnacknowledgedCountRes,
)