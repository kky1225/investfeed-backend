package com.example.investfeed.domain.notification.scheduler

import com.example.investfeed.domain.cryptointerest.repository.CryptoInterestGroupRepository
import com.example.investfeed.domain.cryptointerest.repository.CryptoInterestItemRepository
import com.example.investfeed.domain.interest.repository.InterestGroupRepository
import com.example.investfeed.domain.interest.repository.InterestItemRepository
import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.Direction
import com.example.investfeed.domain.notification.service.NotificationService
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInterestReq
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.upbit.ticker.client.TickerClient
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalTime

@Component
class PriceAlertScheduler(
    private val interestGroupRepository: InterestGroupRepository,
    private val interestItemRepository: InterestItemRepository,
    private val cryptoInterestGroupRepository: CryptoInterestGroupRepository,
    private val cryptoInterestItemRepository: CryptoInterestItemRepository,
    private val stockClient: StockClient,
    private val tickerClient: TickerClient,
    private val notificationService: NotificationService,
    private val holidayService: HolidayService
) {
    private val log = KotlinLogging.logger {}

    companion object {
        val STOCK_THRESHOLDS = listOf(5.0, 10.0, 15.0, 20.0, 30.0)
        val CRYPTO_THRESHOLDS = listOf(5.0, 10.0, 20.0, 30.0)
    }

    @Scheduled(cron = "0 * * * * *")
    fun checkPriceAlerts() {
        val start = System.currentTimeMillis()
        try {
            checkStockAlerts()
        } catch (e: Exception) {
            log.error(e) { "주식 가격 알림 체크 실패" }
        }

        try {
            checkCryptoAlerts()
        } catch (e: Exception) {
            log.error(e) { "암호화폐 가격 알림 체크 실패" }
        }

        log.info { "PriceAlertScheduler 실행 완료: ${System.currentTimeMillis() - start}ms" }
    }

    private fun checkStockAlerts() {
        if (holidayService.isHoliday()) {
            return
        }

        val now = LocalTime.now()
        if (now.isBefore(LocalTime.of(9, 0)) || now.isAfter(LocalTime.of(15, 30))) {
            return
        }

        val allGroups = interestGroupRepository.findAll()
        if (allGroups.isEmpty()) return

        val groupIds = allGroups.map { it.id }
        val allItems = interestItemRepository.findByGroupIdIn(groupIds)
        if (allItems.isEmpty()) return

        val groupToMember = allGroups.associate { it.id to it.memberId }

        val uniqueStkCds = allItems.map { it.stkCd }.distinct()
        val stkCdParam = uniqueStkCds.joinToString("|")

        val kiwoomStockInterestRes = stockClient.stockInterest(KiwoomStockInterestReq(stk_cd = stkCdParam))
        val stockDataMap = kiwoomStockInterestRes.atn_stk_infr?.associateBy { it.stk_cd } ?: return

        val memberStockSet = mutableSetOf<Pair<Long, String>>()

        for (item in allItems) {
            val memberId = groupToMember[item.groupId] ?: continue
            val key = Pair(memberId, item.stkCd)
            if (!memberStockSet.add(key)) continue

            val stockData = stockDataMap[item.stkCd] ?: continue
            val basePric = kotlin.math.abs(stockData.base_pric?.toDoubleOrNull() ?: continue)
            if (basePric == 0.0) continue
            val highPric = kotlin.math.abs(stockData.high_pric?.toDoubleOrNull() ?: continue)
            val lowPric = kotlin.math.abs(stockData.low_pric?.toDoubleOrNull() ?: continue)

            val maxUpRt = ((highPric - basePric) / basePric) * 100
            val maxDownRt = ((lowPric - basePric) / basePric) * 100

            if (maxUpRt > 0) {
                checkThresholds(memberId, AssetType.STOCK, item.stkCd, item.stkNm, maxUpRt, Direction.UP, STOCK_THRESHOLDS)
            }

            if (maxDownRt < 0) {
                checkThresholds(memberId, AssetType.STOCK, item.stkCd, item.stkNm, maxDownRt, Direction.DOWN, STOCK_THRESHOLDS)
            }
        }
    }

    private fun checkCryptoAlerts() {
        val allGroups = cryptoInterestGroupRepository.findAll()
        if (allGroups.isEmpty()) return

        val groupIds = allGroups.map { it.id }
        val allItems = cryptoInterestItemRepository.findByGroupIdIn(groupIds)
        if (allItems.isEmpty()) return

        val groupToMember = allGroups.associate { it.id to it.memberId }

        val uniqueMarkets = allItems.map { it.market }.distinct()
        val marketsParam = uniqueMarkets.joinToString(",")

        val tickers = tickerClient.getTickers(marketsParam)
        val tickerMap = tickers.associateBy { it.market }

        val memberCryptoSet = mutableSetOf<Pair<Long, String>>()

        for (item in allItems) {
            val memberId = groupToMember[item.groupId] ?: continue
            val key = Pair(memberId, item.market)
            if (!memberCryptoSet.add(key)) continue

            val ticker = tickerMap[item.market] ?: continue
            val prevClosing = ticker.prev_closing_price ?: continue
            if (prevClosing == 0.0) continue
            val highPrice = ticker.high_price ?: continue
            val lowPrice = ticker.low_price ?: continue

            val maxUpRt = ((highPrice - prevClosing) / prevClosing) * 100
            val maxDownRt = ((lowPrice - prevClosing) / prevClosing) * 100

            if (maxUpRt > 0) {
                checkThresholds(memberId, AssetType.CRYPTO, item.market, item.koreanName, maxUpRt, Direction.UP, CRYPTO_THRESHOLDS)
            }

            if (maxDownRt < 0) {
                checkThresholds(memberId, AssetType.CRYPTO, item.market, item.koreanName, maxDownRt, Direction.DOWN, CRYPTO_THRESHOLDS)
            }
        }
    }

    private fun checkThresholds(
        memberId: Long,
        assetType: AssetType,
        assetCode: String,
        assetName: String,
        fluRt: Double,
        direction: Direction,
        thresholds: List<Double>
    ) {
        val absFluRt = kotlin.math.abs(fluRt)

        for (threshold in thresholds) {
            if (absFluRt >= threshold) {
                notificationService.createPriceAlert(
                    memberId = memberId,
                    assetType = assetType,
                    assetCode = assetCode,
                    assetName = assetName,
                    threshold = threshold,
                    direction = direction,
                    fluRt = fluRt
                )
            }
        }
    }
}
