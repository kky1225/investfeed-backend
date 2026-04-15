package com.example.investfeed.domain.holding.dto.req

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero

data class ManualHoldingCreateReq(
    @field:Positive(message = "증권사를 선택해주세요.")
    val brokerId: Long,
    @field:NotBlank(message = "종목을 선택해주세요.")
    val stkCd: String,
    @field:NotBlank(message = "종목명을 확인해주세요.")
    val stkNm: String,
    @field:Positive(message = "매수단가를 입력해주세요.")
    val purPrice: Long,
    @field:Positive(message = "수량을 입력해주세요.")
    val quantity: Long,
    @field:Positive(message = "투자원금을 입력해주세요.")
    val purAmt: Long
)

data class ManualHoldingUpdateReq(
    @field:Positive(message = "매수단가를 입력해주세요.")
    val purPrice: Long,
    @field:Positive(message = "수량을 입력해주세요.")
    val quantity: Long,
    @field:Positive(message = "투자원금을 입력해주세요.")
    val purAmt: Long
)

data class HoldingReorderReq(
    val orderedIds: List<Long>
)

data class MemberBrokerBalanceUpdateReq(
    @field:PositiveOrZero(message = "잔액은 0 이상이어야 합니다.")
    val balance: Long
)
