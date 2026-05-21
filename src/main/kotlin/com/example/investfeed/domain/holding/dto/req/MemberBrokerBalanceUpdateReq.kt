package com.example.investfeed.domain.holding.dto.req

import jakarta.validation.constraints.PositiveOrZero

data class MemberBrokerBalanceUpdateReq(
    @field:PositiveOrZero(message = "잔액은 0 이상이어야 합니다.")
    val balance: Long
)