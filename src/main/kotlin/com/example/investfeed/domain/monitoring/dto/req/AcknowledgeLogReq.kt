package com.example.investfeed.domain.monitoring.dto.req

import jakarta.validation.constraints.Size

data class AcknowledgeLogReq(
    @field:Size(max = 500)
    val note: String? = null,
)
