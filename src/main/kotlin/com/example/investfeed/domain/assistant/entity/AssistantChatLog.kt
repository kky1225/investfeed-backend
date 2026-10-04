package com.example.investfeed.domain.assistant.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "assistant_chat_log")
class AssistantChatLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "request_id", nullable = false, length = 64, unique = true)
    val requestId: String,

    @Column(nullable = false, length = 20)
    val route: String,          // TOOL / REJECT / ASK_USER / PICK / BLOCKED / ERROR

    @Column(length = 20)
    val reason: String? = null,

    @Column(length = 300)
    val tools: String? = null,  // 도구 이름 쉼표 구분

    @Column(name = "tool_error_count", nullable = false)
    val toolErrorCount: Short = 0,

    @Column(name = "llm_calls", nullable = false)
    val llmCalls: Short = 0,

    @Column(length = 50)
    val model: String? = null,

    @Column(name = "input_tokens", nullable = false)
    val inputTokens: Int = 0,

    @Column(name = "output_tokens", nullable = false)
    val outputTokens: Int = 0,

    @Column(name = "cache_read_tokens", nullable = false)
    val cacheReadTokens: Int = 0,

    @Column(name = "cache_write_tokens", nullable = false)
    val cacheWriteTokens: Int = 0,

    @Column(name = "cost_usd", nullable = false, precision = 10, scale = 6)
    val costUsd: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    val compacted: Boolean = false,

    @Column(name = "latency_ms", nullable = false)
    val latencyMs: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
