package com.example.investfeed.domain.auth.dto.res

import java.time.LocalDateTime

data class ApiKeyRes(
    val id: Long,
    val brokerId: Long,
    val brokerName: String,
    val appKey: String,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime
)
