package com.example.investfeed.domain.calendar.dto.req

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class BulkRefreshReq(
    @field:Min(2000)
    @field:Max(2100)
    val yearFrom: Int,

    @field:Min(2000)
    @field:Max(2100)
    val yearTo: Int,
)
