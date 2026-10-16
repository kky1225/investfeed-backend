package com.example.investfeed.domain.marketevent.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "market_event")
class MarketEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "trade_date", nullable = false)
    val tradeDate: LocalDate,

    @Column(name = "index_code", nullable = false, length = 10)
    val indexCode: String,                    // KOSPI / KOSDAQ

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    val eventType: MarketEventType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    val direction: MarketEventDirection,      // 서킷브레이커는 항상 DOWN, 사이드카는 매수=UP·매도=DOWN

    @Column(nullable = false)
    val stage: Int,                           // 서킷브레이커 1~3, 단계가 없는 사건은 1

    @Column(name = "trigger_rate", nullable = false)
    val triggerRate: Double,                  // 판정 근거 등락률 (%)

    @Column(name = "trigger_value", nullable = false)
    val triggerValue: Double,                 // 판정 근거 값 (서킷브레이커=지수, 사이드카=선물 가격)

    @Column(name = "started_at", nullable = false)
    val startedAt: LocalDateTime,             // 발동 시각

    @Column(name = "ended_at")
    val endedAt: LocalDateTime?,              // 효력 종료(재개) 예정 시각. 당일 장 종료면 null

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

enum class MarketEventType { CIRCUIT_BREAKER, SIDECAR }

enum class MarketEventDirection { UP, DOWN }
