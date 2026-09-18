package com.example.investfeed.domain.assistant.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(
    name = "assistant_briefing_personal",
    uniqueConstraints = [UniqueConstraint(columnNames = ["briefing_id", "member_id"])]
)
class AssistantBriefingPersonal(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "briefing_id", nullable = false)
    val briefingId: Long,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "headline_text", length = 300)
    val headlineText: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    val body: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fact_sheet", columnDefinition = "jsonb")
    var factSheet: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
