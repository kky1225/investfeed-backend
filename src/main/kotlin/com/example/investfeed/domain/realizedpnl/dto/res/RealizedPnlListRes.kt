package com.example.investfeed.domain.realizedpnl.dto.res

data class RealizedPnlListRes(
    val items: List<RealizedPnlItem>,
    val totalRealizedPnl: Long
)