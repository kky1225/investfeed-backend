package com.example.investfeed.domain.goal.dto.req

import com.example.investfeed.domain.goal.entity.GoalType
import jakarta.validation.constraints.Positive

data class InvestmentGoalCreateReq(
    val type: GoalType,
    @field:Positive(message = "목표 금액을 입력해주세요.")
    val targetAmount: Long
)

data class InvestmentGoalUpdateReq(
    @field:Positive(message = "목표 금액을 입력해주세요.")
    val targetAmount: Long
)
