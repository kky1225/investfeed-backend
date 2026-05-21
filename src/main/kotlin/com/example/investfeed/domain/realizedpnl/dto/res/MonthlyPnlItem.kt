package com.example.investfeed.domain.realizedpnl.dto.res

data class MonthlyPnlItem(
    val year: Int,
    val month: Int,
    val stockPnl: Long,
    val cryptoPnl: Long,
    val totalPnl: Long
)