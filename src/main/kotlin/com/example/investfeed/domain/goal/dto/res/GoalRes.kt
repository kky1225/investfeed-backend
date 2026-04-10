package com.example.investfeed.domain.goal.dto.res

data class InvestmentGoalRes(
    val id: Long,
    val type: String,
    val targetAmount: Long,
    val currentAmount: Long,
    val achievementRate: Double,
    val isAchieved: Boolean,
    val createdAt: String
)

data class GoalDashboardRes(
    val goals: List<InvestmentGoalRes>
)
