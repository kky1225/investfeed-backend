package com.example.investfeed.domain.monitoring.dto.req

import jakarta.validation.constraints.Size

data class BulkAcknowledgeReq(
    @field:Size(max = 500)
    val note: String? = null,
    val ids: List<Long>? = null,
)
