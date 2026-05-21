package com.example.investfeed.domain.goal.dto.req

import jakarta.validation.constraints.Positive

data class InvestmentGoalUpdateReq(
    @field:Positive(message = "목표 금액을 입력해주세요.")
    val targetAmount: Long
)