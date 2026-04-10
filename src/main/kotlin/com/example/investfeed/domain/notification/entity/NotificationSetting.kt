package com.example.investfeed.domain.notification.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "notification_settings")
class NotificationSetting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "member_id", nullable = false, unique = true)
    val memberId: Long,

    @Column(name = "price_up_enabled", nullable = false)
    var priceUpEnabled: Boolean = true,

    @Column(name = "price_down_enabled", nullable = false)
    var priceDownEnabled: Boolean = true,

    @Column(name = "high_52w_enabled", nullable = false)
    var high52wEnabled: Boolean = true,

    @Column(name = "low_52w_enabled", nullable = false)
    var low52wEnabled: Boolean = true,

    @Column(name = "upper_limit_enabled", nullable = false)
    var upperLimitEnabled: Boolean = true,

    @Column(name = "lower_limit_enabled", nullable = false)
    var lowerLimitEnabled: Boolean = true,

    @Column(name = "goal_enabled", nullable = false)
    var goalEnabled: Boolean = true,

    @Column(name = "rebalancing_enabled", nullable = false)
    var rebalancingEnabled: Boolean = true,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
