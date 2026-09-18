package com.example.investfeed.domain.assistant.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "assistant_message")
class AssistantMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(nullable = false, length = 20)
    val type: String, // BRIEFING / ALERT / USER / ASSISTANT / SYSTEM

    @Column(length = 20)
    val subtype: String? = null,

    @Column(name = "as_of")
    val asOf: LocalDateTime? = null,

    @Column(name = "headline_text", nullable = false, length = 300)
    val headlineText: String,

    @Column(name = "headline_scope", nullable = false, length = 10)
    val headlineScope: String = "MARKET",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    val body: String,

    @Column(name = "ref_briefing_id")
    val refBriefingId: Long? = null,

    @Column(name = "ref_personal_briefing_id")
    val refPersonalBriefingId: Long? = null,

    @Column(name = "ref_alert_id")
    val refAlertId: Long? = null,

    @Column(name = "request_id", length = 64)
    val requestId: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
