package com.example.investfeed.domain.realizedpnl.dto.res

data class BrokerRealizedPnlItem(
    val brokerName: String,
    val brokerId: Long,
    val market: String,
    val currentMonthPnl: Long,
    val ytdPnl: Long,
    val allTimePnl: Long
)