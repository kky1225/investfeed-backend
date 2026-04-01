package com.example.investfeed.domain.holding.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "member_brokers")
class MemberBroker(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id", nullable = false)
    val broker: Broker,

    @Column(name = "order_index", nullable = false)
    var orderIndex: Int = 0,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
