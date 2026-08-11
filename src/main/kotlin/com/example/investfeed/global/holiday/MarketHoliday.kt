package com.example.investfeed.global.holiday

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "market_holiday",
    uniqueConstraints = [UniqueConstraint(columnNames = ["market", "dt", "name"])]
)
class MarketHoliday(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 5)
    val market: String, // KR (미장 판정 필요 시 US 확장)

    @Column(nullable = false, length = 8)
    val dt: String, // YYYYMMDD

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(nullable = false, length = 20)
    val source: String, // API:공공데이터

    val createdAt: LocalDateTime = LocalDateTime.now(),
)
