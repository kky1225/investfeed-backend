package com.example.investfeed.domain.realizedpnl.dto.res

data class RealizedPnlSummaryRes(
    val monthly: List<MonthlyPnlItem>,
    val yearlyTotal: Long,
    val allTimeTotal: Long,
    val stockTotal: Long,
    val cryptoTotal: Long
)