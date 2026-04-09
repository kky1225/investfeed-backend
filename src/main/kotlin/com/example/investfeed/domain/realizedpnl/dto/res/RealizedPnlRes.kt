package com.example.investfeed.domain.realizedpnl.dto.res

data class RealizedPnlListRes(
    val items: List<RealizedPnlItem>,
    val totalRealizedPnl: Long
)

data class RealizedPnlItem(
    val id: Long,
    val brokerName: String,
    val brokerId: Long,
    val market: String,
    val year: Int,
    val month: Int,
    val realizedPnl: Long,
    val totalBuyAmt: Long?,
    val totalSellAmt: Long?,
    val tradeFee: Long?,
    val tradeTax: Long?,
    val source: String
)

data class RealizedPnlSummaryRes(
    val monthly: List<MonthlyPnlItem>,
    val yearlyTotal: Long,
    val allTimeTotal: Long,
    val stockTotal: Long,
    val cryptoTotal: Long
)

data class MonthlyPnlItem(
    val year: Int,
    val month: Int,
    val stockPnl: Long,
    val cryptoPnl: Long,
    val totalPnl: Long
)

data class RealizedPnlDashboardItem(
    val currentMonthPnl: Long,
    val ytdPnl: Long,
    val allTimePnl: Long,
    val brokerPnlList: List<BrokerRealizedPnlItem>
)

data class BrokerRealizedPnlItem(
    val brokerName: String,
    val brokerId: Long,
    val market: String,
    val currentMonthPnl: Long,
    val ytdPnl: Long,
    val allTimePnl: Long
)
