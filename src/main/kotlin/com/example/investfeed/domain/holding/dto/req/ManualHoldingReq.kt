package com.example.investfeed.domain.holding.dto.req

data class ManualHoldingCreateReq(
    val brokerId: Long,
    val stkCd: String,
    val stkNm: String,
    val purPrice: Long,
    val quantity: Long,
    val purAmt: Long
)

data class ManualHoldingUpdateReq(
    val purPrice: Long,
    val quantity: Long,
    val purAmt: Long
)
