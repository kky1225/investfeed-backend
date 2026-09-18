package com.example.investfeed.domain.assistant.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "assistant_briefing",
    uniqueConstraints = [UniqueConstraint(columnNames = ["briefing_date", "type", "version"])]
)
class AssistantBriefing(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "briefing_date", nullable = false)
    val briefingDate: LocalDate,

    @Column(nullable = false, length = 10)
    val type: String, // US_CLOSE / KR_PRE / COIN_DAILY / KR_CLOSE / KR_ACCOUNT

    @Column(nullable = false)
    val version: Int = 1,

    @Column(name = "as_of", nullable = false)
    val asOf: LocalDateTime,

    @Column(name = "headline_text", nullable = false, length = 300)
    val headlineText: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    val body: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fact_sheet", columnDefinition = "jsonb")
    var factSheet: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
