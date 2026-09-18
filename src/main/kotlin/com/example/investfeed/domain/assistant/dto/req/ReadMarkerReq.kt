package com.example.investfeed.domain.assistant.dto.req

import jakarta.validation.constraints.Min

data class ReadMarkerReq(
    @field:Min(0)
    val lastSeenId: Long,
)
