package com.example.investfeed.domain.notification.scheduler

import com.example.investfeed.common.util.MarketTimeUtil
import com.example.investfeed.domain.cryptointerest.repository.CryptoInterestGroupRepository
import com.example.investfeed.domain.cryptointerest.repository.CryptoInterestItemRepository
import com.example.investfeed.domain.holding.repository.MemberHoldingRepository
import com.example.investfeed.domain.interest.repository.InterestGroupRepository
import com.example.investfeed.domain.interest.repository.InterestItemRepository
import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.Direction
import com.example.investfeed.domain.notification.entity.PriceTargetDirection
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.monitoring.service.SchedulerType
import com.example.investfeed.domain.notification.service.NotificationService
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomNewHighLowReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInterestReq
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
    private val notificationSettingService: com.example.investfeed.domain.notification.service.NotificationSettingService,
    private val priceTargetRepository: com.example.investfeed.domain.notification.repository.PriceTargetRepository,
    private val holidayService: HolidayService,
    private val memberHoldingRepository: MemberHoldingRepository,
    private val authClient: AuthClient,
    private val schedulerLogService: SchedulerLogService,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String
) {
    private val log = KotlinLogging.logger {}

    companion object {
        val STOCK_THRESHOLDS = listOf(5.0, 10.0, 15.0, 20.0)
        val CRYPTO_THRESHOLDS = listOf(5.0, 10.0, 20.0, 30.0)
    }

    @Scheduled(cron = "0 * * * * *", scheduler = "fastScheduler")
    fun checkPriceAlerts() {
        schedulerLogService.execute("PriceAlertScheduler", SchedulerType.FAST) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
            } catch (e: Exception) {
                log.error(e) { "스케줄러 토큰 발급 실패" }
                SecurityContextHolder.clearContext()
                return@execute
            }

            val start = System.currentTimeMillis()
            var stockDataMap: Map<String, com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockInterest>? = null
            var tickerMap: Map<String?, com.example.investfeed.upbit.ticker.dto.res.UpbitTickerRes>? = null

            try {
                stockDataMap = checkStockAlerts()
            } catch (e: Exception) {
                log.error(e) { "주식 가격 알림 체크 실패" }
            }

            try {
                tickerMap = checkCryptoAlerts()
            } catch (e: Exception) {
                log.error(e) { "암호화폐 가격 알림 체크 실패" }
            }

            try {
                checkStockPriceTargets(stockDataMap ?: emptyMap())
            } catch (e: Exception) {
                log.error(e) { "주식 목표가 알림 체크 실패" }
            }

            try {
                checkCryptoPriceTargets(tickerMap ?: emptyMap())
            } catch (e: Exception) {
                log.error(e) { "암호화폐 목표가 알림 체크 실패" }
            }

            SecurityContextHolder.clearContext()
            log.info { "PriceAlertScheduler 실행 완료: ${System.currentTimeMillis() - start}ms" }
        }
    }

    private fun checkStockAlerts(): Map<String, com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockInterest> {
        if (holidayService.isHoliday()) {
            return emptyMap()
        }

        if (!MarketTimeUtil.isStockAlertTime()) {
            return emptyMap()
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

        if (memberStocks.isEmpty()) return emptyMap()

        val uniqueStkCds = memberStocks.map { it.stkCd }.distinct()
        val stkCdParam = uniqueStkCds.joinToString("|")

        val kiwoomStockInterestRes = stockClient.stockInterest(KiwoomStockInterestReq(stk_cd = stkCdParam))
        val stockDataMap = kiwoomStockInterestRes.atn_stk_infr?.associateBy { it.stk_cd ?: "" } ?: return emptyMap()

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

        // 250일 신고가/신저가 체크 (ka10016)
        try {
            val newHighRes = stockClient.newHighLow(KiwoomNewHighLowReq(ntl_tp = "1"))
            val newHighCodes = newHighRes.ntl_pric?.map { it.stk_cd }?.toSet() ?: emptySet()
            log.info { "250일 신고가 종목 수: ${newHighCodes.size}, 종목: ${newHighCodes.take(10)}" }

            val newLowRes = stockClient.newHighLow(KiwoomNewHighLowReq(ntl_tp = "2"))
            val newLowCodes = newLowRes.ntl_pric?.map { it.stk_cd }?.toSet() ?: emptySet()
            log.info { "250일 신저가 종목 수: ${newLowCodes.size}, 종목: ${newLowCodes.take(10)}" }

            val processedSet = mutableSetOf<Pair<Long, String>>()
            for (item in memberStocks) {
                val key = Pair(item.memberId, item.stkCd)
                if (!processedSet.add(key)) continue

                if (item.stkCd in newHighCodes) {
                    val highPric = kotlin.math.abs(stockDataMap[item.stkCd]?.high_pric?.toDoubleOrNull() ?: 0.0)
                    checkThresholds(item.memberId, AssetType.STOCK, item.stkCd, item.stkNm, highPric, Direction.HIGH_52W, listOf(0.0))
                }
                if (item.stkCd in newLowCodes) {
                    val lowPric = kotlin.math.abs(stockDataMap[item.stkCd]?.low_pric?.toDoubleOrNull() ?: 0.0)
                    checkThresholds(item.memberId, AssetType.STOCK, item.stkCd, item.stkNm, lowPric, Direction.LOW_52W, listOf(0.0))
                }
            }
        } catch (e: Exception) {
            log.warn { "250일 신고저가 체크 실패: ${e.message}" }
        }

        return stockDataMap
    }

    private fun checkCryptoAlerts(): Map<String?, com.example.investfeed.upbit.ticker.dto.res.UpbitTickerRes> {
        val allGroups = cryptoInterestGroupRepository.findAll()
        if (allGroups.isEmpty()) return emptyMap()

        val groupIds = allGroups.map { it.id }
        val allItems = cryptoInterestItemRepository.findByGroupIdIn(groupIds)
        if (allItems.isEmpty()) return emptyMap()

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

            // 52주 신고가/신저가 체크
            val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
            if (ticker.highest_52_week_date == today) {
                checkThresholds(memberId, AssetType.CRYPTO, item.market, item.koreanName, ticker.highest_52_week_price ?: 0.0, Direction.HIGH_52W, listOf(0.0))
            }
            if (ticker.lowest_52_week_date == today) {
                checkThresholds(memberId, AssetType.CRYPTO, item.market, item.koreanName, ticker.lowest_52_week_price ?: 0.0, Direction.LOW_52W, listOf(0.0))
            }
        }

        return tickerMap
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
        // 알림 설정 체크
        val setting = notificationSettingService.getSettingByMemberId(memberId)
        when (direction) {
            Direction.UP -> if (!setting.priceUpEnabled) return
            Direction.DOWN -> if (!setting.priceDownEnabled) return
            Direction.UPPER_LIMIT -> if (!setting.upperLimitEnabled) return
            Direction.LOWER_LIMIT -> if (!setting.lowerLimitEnabled) return
            Direction.HIGH_52W -> if (!setting.high52wEnabled) return
            Direction.LOW_52W -> if (!setting.low52wEnabled) return
            else -> {}
        }

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

    private fun checkStockPriceTargets(stockDataMap: Map<String, com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockInterest>) {
        val targets = priceTargetRepository.findByAssetType(AssetType.STOCK)
        if (targets.isEmpty()) return

        // stockDataMap에 없는 종목은 별도 조회
        val missingCodes = targets.map { it.assetCode }.filter { it !in stockDataMap }.distinct()
        val additionalMap = if (missingCodes.isNotEmpty()) {
            try {
                val stkCdParam = missingCodes.joinToString("|")
                val res = stockClient.stockInterest(KiwoomStockInterestReq(stk_cd = stkCdParam))
                res.atn_stk_infr?.associateBy { it.stk_cd ?: "" } ?: emptyMap()
            } catch (e: Exception) {
                log.warn { "목표가 주식 현재가 조회 실패: ${e.message}" }
                emptyMap()
            }
        } else emptyMap()

        val combinedMap = stockDataMap + additionalMap

        for (target in targets) {
            val stockData = combinedMap[target.assetCode] ?: continue
            val curPrc = kotlin.math.abs(stockData.cur_prc?.toDoubleOrNull() ?: continue)

            val reached = when (target.direction) {
                PriceTargetDirection.ABOVE -> curPrc >= target.targetPrice
                PriceTargetDirection.BELOW -> curPrc <= target.targetPrice
            }

            if (reached) {
                notificationService.createPriceTargetAlert(target, curPrc)
            }
        }
    }

    private fun checkCryptoPriceTargets(tickerMap: Map<String?, com.example.investfeed.upbit.ticker.dto.res.UpbitTickerRes>) {
        val targets = priceTargetRepository.findByAssetType(AssetType.CRYPTO)
        if (targets.isEmpty()) return

        // tickerMap에 없는 종목은 별도 조회
        val missingMarkets = targets.map { it.assetCode }.filter { it !in tickerMap }.distinct()
        val additionalMap = if (missingMarkets.isNotEmpty()) {
            try {
                tickerClient.getTickers(missingMarkets.joinToString(","))
                    .associateBy { it.market }
            } catch (e: Exception) {
                log.warn { "목표가 코인 현재가 조회 실패: ${e.message}" }
                emptyMap()
            }
        } else emptyMap()

        val combinedMap = tickerMap + additionalMap

        for (target in targets) {
            val ticker = combinedMap[target.assetCode] ?: continue
            val tradePrice = ticker.trade_price ?: continue

            val reached = when (target.direction) {
                PriceTargetDirection.ABOVE -> tradePrice >= target.targetPrice
                PriceTargetDirection.BELOW -> tradePrice <= target.targetPrice
            }

            if (reached) {
                notificationService.createPriceTargetAlert(target, tradePrice)
            }
        }
    }

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
