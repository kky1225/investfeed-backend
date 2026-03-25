package com.example.investfeed.domain.auth.dto.res

import java.time.LocalDateTime

data class ApiKeyRes(
    val id: Long,
    val provider: String,
    val appKey: String,
    val createdAt: LocalDateTime
)
