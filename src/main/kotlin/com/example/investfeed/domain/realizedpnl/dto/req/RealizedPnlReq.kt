package com.example.investfeed.domain.realizedpnl.dto.req

data class ManualRealizedPnlCreateReq(
    val brokerId: Long,
    val year: Int,
    val month: Int,
    val realizedPnl: Long
)

data class ManualRealizedPnlUpdateReq(
    val realizedPnl: Long
)

data class RealizedPnlSyncReq(
    val year: Int? = null,
    val month: Int? = null
)

data class RealizedPnlListReq(
    val year: Int? = null,
    val month: Int? = null
)

data class RealizedPnlSummaryReq(
    val year: Int? = null
)
