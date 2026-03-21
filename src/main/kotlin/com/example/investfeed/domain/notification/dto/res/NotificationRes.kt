package com.example.investfeed.domain.notification.dto.res

import com.example.investfeed.domain.notification.entity.*
import java.time.LocalDateTime

data class NotificationRes(
    val id: Long,
    val type: NotificationType,
    val assetType: AssetType,
    val assetCode: String,
    val assetName: String,
    val threshold: Double,
    val direction: Direction,
    val fluRt: Double,
    val isRead: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(notification: Notification) = NotificationRes(
            id = notification.id,
            type = notification.type,
            assetType = notification.assetType,
            assetCode = notification.assetCode,
            assetName = notification.assetName,
            threshold = notification.threshold,
            direction = notification.direction,
            fluRt = notification.fluRt,
            isRead = notification.isRead,
            createdAt = notification.createdAt
        )
    }
}
