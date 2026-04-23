package com.example.investfeed.domain.monitoring.dto.req

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class UpdateSchedulerTimeoutReq(
    @field:Min(1)
    @field:Max(86400)
    val timeoutSec: Int,

    @field:Size(max = 500)
    val reason: String? = null,
)
