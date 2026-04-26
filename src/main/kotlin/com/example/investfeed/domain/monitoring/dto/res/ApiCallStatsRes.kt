package com.example.investfeed.domain.monitoring.dto.res

data class ApiCallStatsRes(
    val items: List<ApiCallStatsItemRes>,
)

data class ApiCallStatsItemRes(
    val provider: String,
    val label: String,
    val todayCount: Long,
    val dailyLimit: Long?,
    val usageRatio: Double?,
    val recent7Days: List<DailyCallCount>,
)

data class DailyCallCount(
    val date: String,
    val count: Long,
)
