package com.example.investfeed.domain.notification.service

import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.Notification
import com.example.investfeed.domain.notification.entity.NotificationType
import com.example.investfeed.domain.us.stock.repository.UsStockMasterRepository
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class NotificationLinkResolver(
    private val usStockMasterRepository: UsStockMasterRepository,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val ASSET_TYPES = setOf(NotificationType.PRICE, NotificationType.TRADE, NotificationType.TARGET_PRICE)
    }

    fun link(notification: Notification): String? = links(listOf(notification))[notification.id]

    fun links(notifications: List<Notification>): Map<Long, String> {
        val tickers = notifications.filter { isUs(it) }.map { it.assetCode.removeSuffix(UsHoldingAlertChecker.US_SUFFIX) }.distinct()
        val stexByTicker = if (tickers.isEmpty()) emptyMap() else runCatching {
            usStockMasterRepository.findByStkCdIn(tickers).groupBy { it.stkCd }.mapValues { (_, rows) -> rows.first().stexTp }
        }.onFailure { log.error(it) { "알림 경로용 미국 거래소 구분 조회 실패: $tickers" } }.getOrDefault(emptyMap())

        return notifications.mapNotNull { n -> link(n, stexByTicker)?.let { n.id to it } }.toMap()
    }

    private fun isUs(n: Notification) = n.assetType == AssetType.STOCK && n.assetCode.endsWith(UsHoldingAlertChecker.US_SUFFIX)

    private fun link(n: Notification, stexByTicker: Map<String, String>): String? {
        if (n.type !in ASSET_TYPES) return null
        return when {
            n.assetType == AssetType.CRYPTO -> "/crypto/detail/${n.assetCode}"
            isUs(n) -> n.assetCode.removeSuffix(UsHoldingAlertChecker.US_SUFFIX).let { t -> stexByTicker[t]?.let { "/us-stock/detail/$it/$t" } }
            n.assetType == AssetType.STOCK -> "/stock/detail/${n.assetCode}"
            else -> null
        }
    }
}
