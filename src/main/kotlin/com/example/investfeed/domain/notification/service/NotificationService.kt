package com.example.investfeed.domain.notification.service

import com.example.investfeed.domain.notification.config.NotificationWebSocketHandler
import com.example.investfeed.domain.notification.dto.res.NotificationRes
import com.example.investfeed.domain.notification.entity.*
import com.example.investfeed.domain.notification.repository.NotificationAlertLogRepository
import com.example.investfeed.domain.notification.repository.NotificationRepository
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val alertLogRepository: NotificationAlertLogRepository,
    private val notificationWebSocketHandler: NotificationWebSocketHandler,
    private val objectMapper: ObjectMapper
) {
    private val log = KotlinLogging.logger {}

    fun getNotifications(memberId: Long, assetType: AssetType? = null): List<NotificationRes> {
        val notifications = if (assetType != null) {
            notificationRepository.findByMemberIdAndAssetTypeOrderByCreatedAtDesc(memberId, assetType)
        } else {
            notificationRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
        }
        return notifications.map { NotificationRes.from(it) }
    }

    fun getUnreadCount(memberId: Long): Int {
        return notificationRepository.countByMemberIdAndIsReadFalse(memberId)
    }

    @Transactional
    fun markAsRead(memberId: Long, notificationId: Long) {
        val notification = notificationRepository.findById(notificationId).orElse(null) ?: return
        if (notification.memberId == memberId) {
            notification.isRead = true
            notificationRepository.save(notification)
        }
    }

    @Transactional
    fun markAllAsRead(memberId: Long) {
        val notifications = notificationRepository.findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(memberId)
        notifications.forEach { it.isRead = true }
        notificationRepository.saveAll(notifications)
    }

    @Transactional
    fun createPriceAlert(
        memberId: Long,
        assetType: AssetType,
        assetCode: String,
        assetName: String,
        threshold: Double,
        direction: Direction,
        fluRt: Double
    ) {
        val today = LocalDate.now()

        val alreadySent = alertLogRepository.existsByMemberIdAndAssetTypeAndAssetCodeAndThresholdAndDirectionAndAlertDate(
            memberId, assetType, assetCode, threshold, direction, today
        )
        if (alreadySent) return

        val notification = Notification(
            memberId = memberId,
            type = NotificationType.PRICE,
            assetType = assetType,
            assetCode = assetCode,
            assetName = assetName,
            threshold = threshold,
            direction = direction,
            fluRt = fluRt
        )
        notificationRepository.save(notification)

        alertLogRepository.save(
            NotificationAlertLog(
                memberId = memberId,
                assetType = assetType,
                assetCode = assetCode,
                threshold = threshold,
                direction = direction,
                alertDate = today
            )
        )

        try {
            val res = NotificationRes.from(notification)
            val message = objectMapper.writeValueAsString(res)
            notificationWebSocketHandler.sendToUser(memberId, message)
        } catch (e: Exception) {
            log.error { "WebSocket 알림 전송 실패: ${e.message}" }
        }
    }
}
