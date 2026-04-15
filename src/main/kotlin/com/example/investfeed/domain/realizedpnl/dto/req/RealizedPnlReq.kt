package com.example.investfeed.domain.realizedpnl.dto.req

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive

data class ManualRealizedPnlCreateReq(
    @field:Positive(message = "증권사/거래소를 선택해주세요.")
    val brokerId: Long,
    @field:Min(value = 2000, message = "연도를 확인해주세요.")
    val year: Int,
    @field:Min(value = 1, message = "월을 확인해주세요.")
    @field:Max(value = 12, message = "월을 확인해주세요.")
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
