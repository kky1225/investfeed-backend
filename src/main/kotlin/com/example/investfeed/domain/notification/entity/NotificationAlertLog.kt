package com.example.investfeed.domain.notification.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "notification_alert_log",
    uniqueConstraints = [
        UniqueConstraint(
            columnNames = ["memberId", "assetType", "assetCode", "threshold", "direction", "alertDate"]
        )
    ]
)
class NotificationAlertLog(
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
    val threshold: Double,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val direction: Direction,

    @Column(nullable = false)
    val alertDate: LocalDate = LocalDate.now()
)
