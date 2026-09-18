package com.example.investfeed.domain.assistant.dto.res

import com.example.investfeed.domain.assistant.dto.message.MessageBody
import java.time.LocalDateTime

data class TimelineMessageRes(
    val id: Long,
    val createdAt: LocalDateTime,
    val body: MessageBody,
)
