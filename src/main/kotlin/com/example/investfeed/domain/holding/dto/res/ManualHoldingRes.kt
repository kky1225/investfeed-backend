package com.example.investfeed.domain.holding.dto.res

data class ManualHoldingListRes(
    val balance: Long = 0, // 계좌 잔액(예수금)
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
    val basePric: String
)
