package com.example.investfeed.domain.notification.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "price_target")
class PriceTarget(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val memberId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val assetType: AssetType,

    @Column(nullable = false)
    val assetCode: String,

    @Column(nullable = false)
    val assetName: String,

    @Column(nullable = false, precision = 18, scale = 4)
    val targetPrice: BigDecimal,   // 국내·코인 원, 미국 달러 (센트 단위 때문에 2026-10-27 NUMERIC)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val direction: PriceTargetDirection,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
