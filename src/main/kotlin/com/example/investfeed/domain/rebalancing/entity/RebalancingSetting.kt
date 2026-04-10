package com.example.investfeed.domain.rebalancing.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "rebalancing_settings")
class RebalancingSetting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "member_id", nullable = false, unique = true)
    val memberId: Long,

    @Column(name = "stock_ratio", nullable = false)
    var stockRatio: Int = 0,

    @Column(name = "stock_direction", nullable = false)
    var stockDirection: String = "MAX", // MIN 또는 MAX

    @Column(name = "crypto_ratio", nullable = false)
    var cryptoRatio: Int = 0,

    @Column(name = "crypto_direction", nullable = false)
    var cryptoDirection: String = "MAX",

    @Column(name = "cash_ratio", nullable = false)
    var cashRatio: Int = 0,

    @Column(name = "cash_direction", nullable = false)
    var cashDirection: String = "MIN",

    @Column(name = "max_stock_ratio", nullable = false)
    var maxStockRatio: Int = 20,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
