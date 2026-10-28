package com.example.investfeed.domain.assistant.dto.req

import jakarta.validation.constraints.NotBlank

data class GoalActionReq(
    @field:NotBlank val cardRef: String,
)
