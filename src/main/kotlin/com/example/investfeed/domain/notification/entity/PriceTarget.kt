package com.example.investfeed.domain.notification.entity

import jakarta.persistence.*
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

    @Column(nullable = false)
    val targetPrice: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val direction: PriceTargetDirection,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
