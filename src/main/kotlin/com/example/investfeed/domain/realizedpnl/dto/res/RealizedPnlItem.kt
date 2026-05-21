package com.example.investfeed.domain.realizedpnl.dto.res

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