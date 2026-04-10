package com.example.investfeed.domain.goal.dto.req

import com.example.investfeed.domain.goal.entity.GoalType

data class InvestmentGoalCreateReq(
    val type: GoalType,
    val targetAmount: Long
)

data class InvestmentGoalUpdateReq(
    val targetAmount: Long
)
