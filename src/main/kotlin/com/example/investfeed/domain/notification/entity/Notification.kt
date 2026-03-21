package com.example.investfeed.domain.notification.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "notification")
class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val memberId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: NotificationType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val assetType: AssetType,

    @Column(nullable = false)
    val assetCode: String,

    @Column(nullable = false)
    val assetName: String,

    @Column(nullable = false)
    val threshold: Double,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val direction: Direction,

    @Column(nullable = false)
    val fluRt: Double,

    @Column(nullable = false)
    var isRead: Boolean = false,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
