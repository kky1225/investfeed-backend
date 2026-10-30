package com.example.investfeed.domain.notification.service

import com.example.investfeed.domain.assistant.dto.factsheet.HoldingAlertHit
import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.Direction
import com.example.investfeed.domain.us.stock.service.UsSessionBaseService
import org.springframework.stereotype.Service
import java.time.LocalDate
import kotlin.math.abs

data class AlertTarget(
    val memberId: Long,
    val assetType: AssetType,
    val code: String,
    val name: String,
    val link: String,
    val held: Boolean,
    val curRate: Double?,
    val curPrice: Double? = null,
)

@Service
class PriceAlertJudge(
    private val notificationSettingService: NotificationSettingService,
    private val notificationService: NotificationService,
) {
    companion object {
        val SINGLE = listOf(0.0)
    }

    fun judge(
        target: AlertTarget,
        direction: Direction,
        fluRt: Double,
        thresholds: List<Double>,
        hits: MutableList<HoldingAlertHit>,
        alertDate: LocalDate = LocalDate.now(),
        extSession: UsSessionBaseService.ExtSession? = null,   // 미국 장외만. 정규장과 따로 센다
    ): Double? {
        val setting = notificationSettingService.getSettingByMemberId(target.memberId)
        val enabled = when (direction) {
            Direction.UP -> setting.priceUpEnabled
            Direction.DOWN -> setting.priceDownEnabled
            Direction.UPPER_LIMIT -> setting.upperLimitEnabled
            Direction.LOWER_LIMIT -> setting.lowerLimitEnabled
            Direction.HIGH_52W -> setting.high52wEnabled
            Direction.LOW_52W -> setting.low52wEnabled
            else -> true
        }
        if (!enabled) return null

        val absFluRt = abs(fluRt)
        val pending = thresholds
            .filter { absFluRt >= it }
            .filterNot { notificationService.isPriceAlertSent(target.memberId, target.assetType, target.code, it, direction, alertDate, extSession) }
        val highestFired = pending.maxOrNull() ?: return null

        if (!notificationService.createPriceAlert(target.memberId, target.assetType, target.code, target.name, highestFired, direction, fluRt, alertDate, target.curPrice, extSession)) return null
        pending.filter { it < highestFired }.forEach {
            notificationService.recordPriceAlertSent(target.memberId, target.assetType, target.code, it, direction, alertDate, extSession)
        }

        if (target.held) {
            val is52w = direction == Direction.HIGH_52W || direction == Direction.LOW_52W
            hits += HoldingAlertHit(
                target.memberId, target.code, target.name, target.link, direction,
                threshold = highestFired, triggerRate = if (is52w) null else fluRt,
                price = if (is52w) fluRt else target.curPrice,
                sessionLabel = extSession?.label,
            )
        }
        return highestFired
    }
}
