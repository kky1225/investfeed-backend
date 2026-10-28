package com.example.investfeed.internal.assistant.dto.req

import com.example.investfeed.domain.goal.entity.GoalType

data class GoalUpdatePreviewReq(
    val type: GoalType,
    val targetAmount: Long,
)
