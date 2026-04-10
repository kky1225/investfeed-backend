package com.example.investfeed.domain.notification.service

import com.example.investfeed.domain.goal.entity.GoalType
import com.example.investfeed.domain.notification.config.NotificationWebSocketHandler
import com.example.investfeed.domain.notification.dto.res.NotificationRes
import com.example.investfeed.domain.notification.entity.*
import com.example.investfeed.domain.notification.repository.PriceTargetRepository
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
    private val priceTargetRepository: PriceTargetRepository,
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

    @Transactional
    fun createPriceTargetAlert(priceTarget: PriceTarget, currentPrice: Double) {
        val notification = Notification(
            memberId = priceTarget.memberId,
            type = NotificationType.TARGET_PRICE,
            assetType = priceTarget.assetType,
            assetCode = priceTarget.assetCode,
            assetName = priceTarget.assetName,
            threshold = priceTarget.targetPrice.toDouble(),
            direction = if (priceTarget.direction == PriceTargetDirection.ABOVE) Direction.TARGET_ABOVE else Direction.TARGET_BELOW,
            fluRt = currentPrice
        )
        notificationRepository.save(notification)

        // 1회 알림 후 자동 삭제
        priceTargetRepository.delete(priceTarget)

        try {
            val res = NotificationRes.from(notification)
            val message = objectMapper.writeValueAsString(res)
            notificationWebSocketHandler.sendToUser(priceTarget.memberId, message)
        } catch (e: Exception) {
            log.error { "목표가 WebSocket 알림 전송 실패: ${e.message}" }
        }
    }

    @Transactional
    fun createGoalAlert(
        memberId: Long,
        goalType: GoalType,
        targetAmount: Long,
        currentAmount: Long
    ) {
        val now = LocalDate.now()

        // 중복 방지: 월간은 월 1번, 연간은 연 1번, 총 자산은 목표 변경 전까지 1번
        val alertDate = when (goalType) {
            GoalType.MONTHLY_REALIZED_PNL -> LocalDate.of(now.year, now.monthValue, 1)
            GoalType.YEARLY_REALIZED_PNL -> LocalDate.of(now.year, 1, 1)
            GoalType.TOTAL_ASSET -> LocalDate.of(2000, 1, 1)
        }
        val alreadySent = alertLogRepository.existsByMemberIdAndAssetTypeAndAssetCodeAndThresholdAndDirectionAndAlertDate(
            memberId, AssetType.TOTAL, goalType.name, targetAmount.toDouble(), Direction.GOAL_ACHIEVED, alertDate
        )
        if (alreadySent) return

        val notification = Notification(
            memberId = memberId,
            type = NotificationType.GOAL,
            assetType = AssetType.TOTAL,
            assetCode = goalType.name,
            assetName = when (goalType) {
                GoalType.TOTAL_ASSET -> "목표 총 자산"
                GoalType.MONTHLY_REALIZED_PNL -> "월간 실현손익 목표"
                GoalType.YEARLY_REALIZED_PNL -> "연간 실현손익 목표"
            },
            threshold = targetAmount.toDouble(),
            direction = Direction.GOAL_ACHIEVED,
            fluRt = currentAmount.toDouble()
        )
        notificationRepository.save(notification)

        alertLogRepository.save(
            NotificationAlertLog(
                memberId = memberId,
                assetType = AssetType.TOTAL,
                assetCode = goalType.name,
                threshold = targetAmount.toDouble(),
                direction = Direction.GOAL_ACHIEVED,
                alertDate = alertDate
            )
        )

        try {
            val res = NotificationRes.from(notification)
            val message = objectMapper.writeValueAsString(res)
            notificationWebSocketHandler.sendToUser(memberId, message)
        } catch (e: Exception) {
            log.error { "투자 목표 WebSocket 알림 전송 실패: ${e.message}" }
        }
    }
}
