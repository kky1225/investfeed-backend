package com.example.investfeed.domain.realizedpnl.dto.res

data class RealizedPnlDashboardItem(
    val currentMonthPnl: Long,
    val ytdPnl: Long,
    val allTimePnl: Long,
    val brokerPnlList: List<BrokerRealizedPnlItem>
)