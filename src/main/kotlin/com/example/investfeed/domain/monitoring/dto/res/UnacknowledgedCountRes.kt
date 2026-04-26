package com.example.investfeed.domain.monitoring.dto.res

data class UnacknowledgedCountRes(
    val schedulerLogs: Long,
    val errorLogs: Long,
)
