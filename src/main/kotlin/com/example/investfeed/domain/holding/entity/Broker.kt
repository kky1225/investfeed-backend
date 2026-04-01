package com.example.investfeed.domain.holding.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "brokers")
class Broker(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: BrokerType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var market: MarketType,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
