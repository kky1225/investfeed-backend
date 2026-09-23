package com.example.investfeed.domain.assistant.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "assistant_release_alert",
    uniqueConstraints = [UniqueConstraint(columnNames = ["country", "event_date", "event_name"])]
)
class AssistantReleaseAlert(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "target_key", nullable = false, length = 30)
    val targetKey: String,

    @Column(nullable = false, length = 5)
    val country: String,

    @Column(name = "event_date", nullable = false)
    val eventDate: LocalDate,

    @Column(name = "event_name", nullable = false, length = 200)
    val eventName: String,

    @Column(nullable = false, length = 100)
    val value: String,

    @Column(name = "prev_value", length = 100)
    val prevValue: String? = null,

    @Column(nullable = false)
    val suppressed: Boolean = false,

    @Column(name = "detected_at", nullable = false)
    val detectedAt: LocalDateTime,

    @Column(name = "headline_text", nullable = false, length = 300)
    val headlineText: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    val body: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
