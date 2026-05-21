package com.example.investfeed.domain.holding.dto.req

import jakarta.validation.constraints.Positive

data class ManualHoldingUpdateReq(
    @field:Positive(message = "매수단가를 입력해주세요.")
    val purPrice: Long,
    @field:Positive(message = "수량을 입력해주세요.")
    val quantity: Long,
    @field:Positive(message = "투자원금을 입력해주세요.")
    val purAmt: Long
)