package com.example.investfeed.domain.notification.scheduler

import com.example.investfeed.domain.cryptointerest.repository.CryptoInterestGroupRepository
import com.example.investfeed.domain.cryptointerest.repository.CryptoInterestItemRepository
import com.example.investfeed.domain.interest.repository.InterestGroupRepository
import com.example.investfeed.domain.interest.repository.InterestItemRepository
import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.Direction
import com.example.investfeed.domain.notification.service.NotificationService
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInterestReq
import com.example.investfeed.common.util.MarketTimeUtil
import com.example.investfeed.domain.holding.repository.MemberHoldingRepository
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.upbit.ticker.client.TickerClient
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class PriceAlertScheduler(
    private val interestGroupRepository: InterestGroupRepository,
    private val interestItemRepository: InterestItemRepository,
    private val cryptoInterestGroupRepository: CryptoInterestGroupRepository,
    private val cryptoInterestItemRepository: CryptoInterestItemRepository,
    private val stockClient: StockClient,
    private val tickerClient: TickerClient,
    private val notificationService: NotificationService,
    private val holidayService: HolidayService,
    private val memberHoldingRepository: MemberHoldingRepository,
    private val authClient: AuthClient,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String
) {
    private val log = KotlinLogging.logger {}

    companion object {
        val STOCK_THRESHOLDS = listOf(5.0, 10.0, 15.0, 20.0)
        val CRYPTO_THRESHOLDS = listOf(5.0, 10.0, 20.0, 30.0)
    }

    @Scheduled(cron = "0 * * * * *")
    fun checkPriceAlerts() {
        setSchedulerSecurityContext()
        try {
            authClient.accessToken()
        } catch (e: Exception) {
            log.error(e) { "스케줄러 토큰 발급 실패" }
            SecurityContextHolder.clearContext()
            return
        }

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

        SecurityContextHolder.clearContext()
        log.info { "PriceAlertScheduler 실행 완료: ${System.currentTimeMillis() - start}ms" }
    }

    private fun checkStockAlerts() {
        if (holidayService.isHoliday()) {
            return
        }

        if (!MarketTimeUtil.isStockAlertTime()) {
            return
        }

        data class MemberStock(val memberId: Long, val stkCd: String, val stkNm: String)

        val memberStocks = mutableListOf<MemberStock>()

        val allGroups = interestGroupRepository.findAll()
        if (allGroups.isNotEmpty()) {
            val groupIds = allGroups.map { it.id }
            val allItems = interestItemRepository.findByGroupIdIn(groupIds)
            val groupToMember = allGroups.associate { it.id to it.memberId }

            allItems.forEach { item ->
                val memberId = groupToMember[item.groupId] ?: return@forEach
                memberStocks.add(MemberStock(memberId, item.stkCd, item.stkNm))
            }
        }

        val allHoldings = memberHoldingRepository.findAll()
        allHoldings.forEach { holding ->
            memberStocks.add(MemberStock(holding.memberId, holding.stkCd, holding.stkNm))
        }

        if (memberStocks.isEmpty()) return

        val uniqueStkCds = memberStocks.map { it.stkCd }.distinct()
        val stkCdParam = uniqueStkCds.joinToString("|")

        val kiwoomStockInterestRes = stockClient.stockInterest(KiwoomStockInterestReq(stk_cd = stkCdParam))
        val stockDataMap = kiwoomStockInterestRes.atn_stk_infr?.associateBy { it.stk_cd } ?: return

        val memberStockSet = mutableSetOf<Pair<Long, String>>()

        for (item in memberStocks) {
            val key = Pair(item.memberId, item.stkCd)
            if (!memberStockSet.add(key)) continue

            val stockData = stockDataMap[item.stkCd] ?: continue
            val basePric = kotlin.math.abs(stockData.base_pric?.toDoubleOrNull() ?: continue)
            if (basePric == 0.0) continue
            val highPric = kotlin.math.abs(stockData.high_pric?.toDoubleOrNull() ?: continue)
            if (highPric == 0.0) continue
            val lowPric = kotlin.math.abs(stockData.low_pric?.toDoubleOrNull() ?: continue)
            if (lowPric == 0.0) continue
            val uplPric = stockData.upl_pric?.toDoubleOrNull()
            val lstPric = stockData.lst_pric?.toDoubleOrNull()

            val maxUpRt = ((highPric - basePric) / basePric) * 100
            val maxDownRt = ((lowPric - basePric) / basePric) * 100

            if (maxUpRt > 0) {
                checkThresholds(item.memberId, AssetType.STOCK, item.stkCd, item.stkNm, maxUpRt, Direction.UP, STOCK_THRESHOLDS)
            }

            if (maxDownRt < 0) {
                checkThresholds(item.memberId, AssetType.STOCK, item.stkCd, item.stkNm, maxDownRt, Direction.DOWN, STOCK_THRESHOLDS)
            }

            if (uplPric != null && highPric >= kotlin.math.abs(uplPric)) {
                checkThresholds(item.memberId, AssetType.STOCK, item.stkCd, item.stkNm, maxUpRt, Direction.UPPER_LIMIT, listOf(0.0))
            }

            if (lstPric != null && lowPric <= kotlin.math.abs(lstPric)) {
                checkThresholds(item.memberId, AssetType.STOCK, item.stkCd, item.stkNm, maxDownRt, Direction.LOWER_LIMIT, listOf(0.0))
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

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
