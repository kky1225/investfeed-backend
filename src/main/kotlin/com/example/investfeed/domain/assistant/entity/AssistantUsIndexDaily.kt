package com.example.investfeed.domain.assistant.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "assistant_us_index_daily",
    uniqueConstraints = [UniqueConstraint(columnNames = ["series", "trade_date"])]
)
class AssistantUsIndexDaily(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 20)
    val series: String, // NASDAQ / SPX / DOW / SOX / VIX / US2Y / US10Y

    @Column(name = "trade_date", nullable = false)
    val tradeDate: LocalDate,

    @Column(nullable = false)
    var value: Double,

    var change: Double? = null,

    @Column(nullable = false, length = 10)
    var source: String, // NAVER / FRED

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
