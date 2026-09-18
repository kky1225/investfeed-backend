package com.example.investfeed.domain.assistant.dto.res

import java.time.LocalDateTime

data class AssistantTokenRes(
    val token: String,
    val expiresAt: LocalDateTime,
    val kid: String,
)
