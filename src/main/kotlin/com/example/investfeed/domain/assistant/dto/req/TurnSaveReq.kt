package com.example.investfeed.domain.assistant.dto.req

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.LocalDateTime

data class TurnSaveReq(
    @field:NotBlank val requestId: String,
    @field:NotBlank val question: String,
    @field:NotBlank val replyText: String,
    @field:Valid val cards: List<TurnCardReq> = emptyList(),
    @field:Valid val log: ChatLogReq,
)

data class TurnCardReq(
    @field:NotBlank val kind: String,
    val ref: String? = null,
    val payload: Any? = null,
    val args: Map<String, Any?>? = null,   // 공개 데이터 카드의 조회 조건 (도구 인자)
    val asOf: LocalDateTime? = null,
    val source: String? = null,
    val error: String? = null,
)

data class ChatLogReq(
    @field:NotBlank val route: String,
    val reason: String? = null,
    val tools: List<String> = emptyList(),
    val toolErrorCount: Int = 0,
    val llmCalls: Int = 0,
    val model: String? = null,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val cacheReadTokens: Int = 0,
    val cacheWriteTokens: Int = 0,
    val costUsd: BigDecimal = BigDecimal.ZERO,
    val compacted: Boolean = false,
    val latencyMs: Int = 0,
)
