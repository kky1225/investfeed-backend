package com.example.investfeed.domain.notification.service

import com.example.investfeed.domain.assistant.dto.factsheet.PriceTargetHit
import com.example.investfeed.domain.assistant.service.UsMarketCalendarService
import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.PriceTargetDirection
import com.example.investfeed.domain.notification.repository.PriceTargetRepository
import com.example.investfeed.domain.us.stock.repository.UsStockMasterRepository
import com.example.investfeed.kiwoom.us.stock.client.UsStockClient
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockInfoReq
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.abs

@Service
class UsPriceTargetChecker(
    private val priceTargetRepository: PriceTargetRepository,
    private val usStockClient: UsStockClient,
    private val usStockMasterRepository: UsStockMasterRepository,
    private val usMarketCalendarService: UsMarketCalendarService,
    private val notificationService: NotificationService,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val PRE_OPEN_ET: LocalTime = LocalTime.of(4, 0)
        private val AFTER_CLOSE_ET: LocalTime = LocalTime.of(20, 0)
        private val DAY_MARKET_OPEN_KST: LocalTime = LocalTime.of(9, 0)     // 서머타임 09~17시, 해제 10~18시 → 넉넉히 09~18시
        private val DAY_MARKET_CLOSE_KST: LocalTime = LocalTime.of(18, 0)
    }

    fun check(hits: MutableList<PriceTargetHit>, now: LocalDateTime) {
        if (!isTradingWindow(now)) return
        val targets = priceTargetRepository.findByAssetType(AssetType.STOCK).filter { it.assetCode.endsWith(UsHoldingAlertChecker.US_SUFFIX) }
        if (targets.isEmpty()) return

        val byTicker = targets.groupBy { it.assetCode.removeSuffix(UsHoldingAlertChecker.US_SUFFIX) }
        val stexByTicker = usStockMasterRepository.findByStkCdIn(byTicker.keys).groupBy { it.stkCd }.mapValues { (_, rows) -> rows.first().stexTp }

        byTicker.forEach { (ticker, tickerTargets) ->
            val stexTp = stexByTicker[ticker] ?: run { log.warn { "미국 목표가 종목 거래소 구분 없음(us_stock_master): $ticker" }; return@forEach }
            val res = try {
                usStockClient.usStockInfo(KiwoomUsStockInfoReq(stex_tp = stexTp, stk_cd = ticker))
            } catch (e: Exception) {
                log.warn { "미국 목표가 현재가 조회 실패: $ticker ${e.message}" }
                return@forEach
            }
            val curPrc = res.cur_prc?.toDoubleOrNull()?.let { abs(it) }?.takeIf { it > 0 } ?: return@forEach

            tickerTargets.forEach { target ->
                val reached = when (target.direction) {
                    PriceTargetDirection.ABOVE -> curPrc >= target.targetPrice.toDouble()
                    PriceTargetDirection.BELOW -> curPrc <= target.targetPrice.toDouble()
                }
                if (reached) {
                    notificationService.createPriceTargetAlert(target, curPrc)
                    hits += PriceTargetHit(
                        target.memberId, target.assetCode, target.assetName, "/us-stock/detail/$stexTp/$ticker",
                        target.direction, target.targetPrice.toDouble(), curPrc,
                    )
                }
            }
        }
    }

    /**
     * 장외 포함 거래 시간: 미국 거래일 ET 04:00~20:00(프리·정규·애프터) + 한국 평일 09:00~18:00(데이마켓, ET 20:00~04:00).
     * 경계는 넉넉히 잡는다 — 장이 쉬는 시간엔 현재가가 멈춰 있어 새로 도달하지 않는다
     */
    private fun isTradingWindow(now: LocalDateTime): Boolean {
        val ny = now.atZone(UsMarketCalendarService.KST).withZoneSameInstant(UsMarketCalendarService.NY)
        val nyTime = ny.toLocalTime()
        if (usMarketCalendarService.isUsTradingDay(ny.toLocalDate()) && !nyTime.isBefore(PRE_OPEN_ET) && nyTime.isBefore(AFTER_CLOSE_ET)) return true
        val kstTime = now.toLocalTime()
        val weekday = now.dayOfWeek != DayOfWeek.SATURDAY && now.dayOfWeek != DayOfWeek.SUNDAY
        return weekday && !kstTime.isBefore(DAY_MARKET_OPEN_KST) && kstTime.isBefore(DAY_MARKET_CLOSE_KST)
    }
}
