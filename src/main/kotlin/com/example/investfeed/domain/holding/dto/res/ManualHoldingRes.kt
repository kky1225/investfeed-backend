package com.example.investfeed.domain.holding.dto.res

data class ManualHoldingListRes(
    val holdings: List<ManualHoldingItem>
)

data class ManualHoldingItem(
    val id: Long,
    val stkCd: String,
    val stkNm: String,
    val purPrice: Long,
    val quantity: Long,
    val purAmt: Long,
    val curPrc: String,
    val fluRt: String,
    val predPre: String,
    val predPreSig: String
)
