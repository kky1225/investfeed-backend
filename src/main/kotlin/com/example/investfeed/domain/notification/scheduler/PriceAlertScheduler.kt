package com.example.investfeed.domain.notification.scheduler

import com.example.investfeed.common.util.MarketTimeUtil
import com.example.investfeed.domain.assistant.dto.factsheet.HoldingAlertHit
import com.example.investfeed.domain.assistant.service.HoldingAlertService
import com.example.investfeed.domain.cryptointerest.repository.CryptoInterestGroupRepository
import com.example.investfeed.domain.cryptointerest.repository.CryptoInterestItemRepository
import com.example.investfeed.domain.holding.entity.MemberHolding
import com.example.investfeed.domain.holding.repository.MemberHoldingRepository
import com.example.investfeed.domain.interest.repository.InterestGroupRepository
import com.example.investfeed.domain.interest.repository.InterestItemRepository
import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.Direction
import com.example.investfeed.domain.notification.entity.PriceTargetDirection
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.notification.service.AlertTarget
import com.example.investfeed.domain.notification.service.NotificationService
import com.example.investfeed.domain.notification.service.PriceAlertJudge
import com.example.investfeed.domain.notification.service.UsHoldingAlertChecker
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
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@Component
class PriceAlertScheduler(
    private val interestGroupRepository: InterestGroupRepository,
    private val interestItemRepository: InterestItemRepository,
    private val cryptoInterestGroupRepository: CryptoInterestGroupRepository,
    private val cryptoInterestItemRepository: CryptoInterestItemRepository,
    private val stockClient: StockClient,
    private val usHoldingAlertChecker: UsHoldingAlertChecker,
    private val tickerClient: TickerClient,
    private val notificationService: NotificationService,
    private val notificationSettingService: com.example.investfeed.domain.notification.service.NotificationSettingService,
    private val judge: PriceAlertJudge,
    private val priceTargetRepository: com.example.investfeed.domain.notification.repository.PriceTargetRepository,
    private val holidayService: HolidayService,
    private val memberHoldingRepository: MemberHoldingRepository,
    private val holdingAlertService: HoldingAlertService,
    private val authClient: AuthClient,
    private val schedulerLogService: SchedulerLogService,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String
) {
    private val log = KotlinLogging.logger {}

    companion object {
        val STOCK_THRESHOLDS = listOf(5.0, 10.0, 15.0, 20.0)                 // 국내: 30% 근처는 상한가·하한가가 맡는다
        val CRYPTO_THRESHOLDS = listOf(5.0, 10.0, 15.0, 20.0, 30.0)          // 15% 추가 (2026-09-25, 주식과 간격 통일)
        private const val KR_SUFFIX = "_AL"
        private const val CRYPTO_PREFIX = "KRW-"
        private val YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    private data class MemberAsset(val memberId: Long, val code: String, val name: String)

    private fun targets(interest: List<MemberAsset>, holdings: List<MemberHolding>): Pair<List<MemberAsset>, Set<Pair<Long, String>>> {
        val held = holdings.map { it.memberId to it.stkCd }.toSet()
        val all = (interest + holdings.map { MemberAsset(it.memberId, it.stkCd, it.stkNm) }).distinctBy { it.memberId to it.code }
        return all to held
    }

    @Scheduled(cron = SchedulerCron.PRICE_ALERT, scheduler = "fastScheduler")
    fun checkPriceAlerts() {
        schedulerLogService.execute(SchedulerName.PriceAlertScheduler) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
            } catch (e: Exception) {
                log.error(e) { "스케줄러 토큰 발급 실패" }
                SecurityContextHolder.clearContext()
                return@execute
            }

            val start = System.currentTimeMillis()
            val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
            val holdings = memberHoldingRepository.findAll()
            val holdingHits = mutableListOf<HoldingAlertHit>()
            var stockDataMap: Map<String, com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockInterest>? = null
            var tickerMap: Map<String?, com.example.investfeed.upbit.ticker.dto.res.UpbitTickerRes>? = null

            try {
                stockDataMap = checkStockAlerts(holdings.filter { it.stkCd.endsWith(KR_SUFFIX) }, holdingHits)
            } catch (e: Exception) {
                log.error(e) { "주식 가격 알림 체크 실패" }
            }

            try {
                tickerMap = checkCryptoAlerts(holdings.filter { it.stkCd.startsWith(CRYPTO_PREFIX) }, holdingHits)
            } catch (e: Exception) {
                log.error(e) { "암호화폐 가격 알림 체크 실패" }
            }

            try {
                usHoldingAlertChecker.check(holdings.filter { it.stkCd.endsWith(UsHoldingAlertChecker.US_SUFFIX) }, holdingHits, now)
            } catch (e: Exception) {
                log.error(e) { "미국 주식 보유 알림 체크 실패" }
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

            try {
                val posted = holdingAlertService.publish(holdingHits, now)
                if (posted > 0) log.info { "보유 종목 급등락 비서 게시: ${posted}건 (hit ${holdingHits.size})" }
            } catch (e: Exception) {
                log.error(e) { "보유 종목 급등락 비서 게시 실패" }
            }

            SecurityContextHolder.clearContext()
            log.info { "PriceAlertScheduler 실행 완료: ${System.currentTimeMillis() - start}ms" }
        }
    }

    private fun checkStockAlerts(
        krHoldings: List<MemberHolding>,
        hits: MutableList<HoldingAlertHit>,
    ): Map<String, com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockInterest> {
        if (holidayService.isHoliday()) {
            return emptyMap()
        }

        if (!MarketTimeUtil.isStockAlertTime()) {
            return emptyMap()
        }

        val groups = interestGroupRepository.findAll()
        val groupToMember = groups.associate { it.id to it.memberId }
        val interest = if (groups.isEmpty()) emptyList() else interestItemRepository.findByGroupIdIn(groups.map { it.id })
            .mapNotNull { item -> groupToMember[item.groupId]?.let { MemberAsset(it, item.stkCd, item.stkNm) } }
        val (memberStocks, heldKeys) = targets(interest, krHoldings)
        if (memberStocks.isEmpty()) return emptyMap()

        val stkCdParam = memberStocks.map { it.code }.distinct().joinToString("|")
        val kiwoomStockInterestRes = runBlocking { stockClient.stockInterest(KiwoomStockInterestReq(stk_cd = stkCdParam)) }
        val stockDataMap = kiwoomStockInterestRes.atn_stk_infr?.associateBy { it.stk_cd ?: "" } ?: return emptyMap()

        fun price(s: String?) = s?.toDoubleOrNull()?.let { abs(it) }?.takeIf { it > 0 }
        fun target(item: MemberAsset, curRate: Double?, curPrice: Double?) = AlertTarget(
            item.memberId, AssetType.STOCK, item.code, item.name, "/stock/detail/${item.code.substringBefore("_")}",
            held = (item.memberId to item.code) in heldKeys, curRate = curRate, curPrice = curPrice,
        )

        for (item in memberStocks) {
            val stockData = stockDataMap[item.code] ?: continue
            val basePric = price(stockData.base_pric) ?: continue
            val highPric = price(stockData.high_pric) ?: continue
            val lowPric = price(stockData.low_pric) ?: continue
            val curPrice = price(stockData.cur_prc)
            val curRate = curPrice?.let { (it - basePric) / basePric * 100 }
            val maxUpRt = (highPric - basePric) / basePric * 100
            val maxDownRt = (lowPric - basePric) / basePric * 100
            val t = target(item, curRate, curPrice)

            val setting = notificationSettingService.getSettingByMemberId(item.memberId)
            val upperReached = price(stockData.upl_pric)?.let { highPric >= it } ?: false
            val lowerReached = price(stockData.lst_pric)?.let { lowPric <= it } ?: false

            if (upperReached) judge.judge(t, Direction.UPPER_LIMIT, maxUpRt, PriceAlertJudge.SINGLE, hits)
            if (lowerReached) judge.judge(t, Direction.LOWER_LIMIT, maxDownRt, PriceAlertJudge.SINGLE, hits)
            if (maxUpRt > 0 && !(upperReached && setting.upperLimitEnabled)) judge.judge(t, Direction.UP, maxUpRt, STOCK_THRESHOLDS, hits)
            if (maxDownRt < 0 && !(lowerReached && setting.lowerLimitEnabled)) judge.judge(t, Direction.DOWN, maxDownRt, STOCK_THRESHOLDS, hits)
        }

        try {
            val newHighRes = runBlocking { stockClient.newHighLow(KiwoomNewHighLowReq(ntl_tp = "1")) }
            val newHighCodes = newHighRes.ntl_pric?.map { it.stk_cd }?.toSet() ?: emptySet()
            log.info { "250일 신고가 종목 수: ${newHighCodes.size}, 종목: ${newHighCodes.take(10)}" }

            val newLowRes = runBlocking { stockClient.newHighLow(KiwoomNewHighLowReq(ntl_tp = "2")) }
            val newLowCodes = newLowRes.ntl_pric?.map { it.stk_cd }?.toSet() ?: emptySet()
            log.info { "250일 신저가 종목 수: ${newLowCodes.size}, 종목: ${newLowCodes.take(10)}" }

            for (item in memberStocks) {
                if (item.code !in newHighCodes && item.code !in newLowCodes) continue
                val stockData = stockDataMap[item.code]
                val basePric = price(stockData?.base_pric)
                val curPrice = price(stockData?.cur_prc)
                val curRate = curPrice?.let { c -> basePric?.let { (c - it) / it * 100 } }
                val t = target(item, curRate, curPrice)
                if (item.code in newHighCodes) judge.judge(t, Direction.HIGH_52W, price(stockData?.high_pric) ?: 0.0, PriceAlertJudge.SINGLE, hits)
                if (item.code in newLowCodes) judge.judge(t, Direction.LOW_52W, price(stockData?.low_pric) ?: 0.0, PriceAlertJudge.SINGLE, hits)
            }
        } catch (e: Exception) {
            log.warn { "250일 신고저가 체크 실패: ${e.message}" }
        }

        return stockDataMap
    }

    private fun checkCryptoAlerts(
        cryptoHoldings: List<MemberHolding>,
        hits: MutableList<HoldingAlertHit>,
    ): Map<String?, com.example.investfeed.upbit.ticker.dto.res.UpbitTickerRes> {
        val groups = cryptoInterestGroupRepository.findAll()
        val groupToMember = groups.associate { it.id to it.memberId }
        val interest = if (groups.isEmpty()) emptyList() else cryptoInterestItemRepository.findByGroupIdIn(groups.map { it.id })
            .mapNotNull { item -> groupToMember[item.groupId]?.let { MemberAsset(it, item.market, item.koreanName) } }
        val (memberCryptos, heldKeys) = targets(interest, cryptoHoldings)
        if (memberCryptos.isEmpty()) return emptyMap()

        val tickerMap = tickerClient.getTickers(memberCryptos.map { it.code }.distinct().joinToString(",")).associateBy { it.market }
        val today = LocalDate.now().format(YYYYMMDD)

        for (item in memberCryptos) {
            val ticker = tickerMap[item.code] ?: continue
            val prevClosing = ticker.prev_closing_price?.takeIf { it > 0 } ?: continue
            val highPrice = ticker.high_price ?: continue
            val lowPrice = ticker.low_price ?: continue
            val curPrice = ticker.trade_price?.takeIf { it > 0 }
            val curRate = curPrice?.let { (it - prevClosing) / prevClosing * 100 }
            val maxUpRt = (highPrice - prevClosing) / prevClosing * 100
            val maxDownRt = (lowPrice - prevClosing) / prevClosing * 100
            val t = AlertTarget(item.memberId, AssetType.CRYPTO, item.code, item.name, "/crypto/detail/${item.code}",
                held = (item.memberId to item.code) in heldKeys, curRate = curRate, curPrice = curPrice)

            if (maxUpRt > 0) judge.judge(t, Direction.UP, maxUpRt, CRYPTO_THRESHOLDS, hits)
            if (maxDownRt < 0) judge.judge(t, Direction.DOWN, maxDownRt, CRYPTO_THRESHOLDS, hits)
            if (ticker.highest_52_week_date == today) judge.judge(t, Direction.HIGH_52W, ticker.highest_52_week_price ?: 0.0, PriceAlertJudge.SINGLE, hits)
            if (ticker.lowest_52_week_date == today) judge.judge(t, Direction.LOW_52W, ticker.lowest_52_week_price ?: 0.0, PriceAlertJudge.SINGLE, hits)
        }

        return tickerMap
    }

    private fun checkStockPriceTargets(stockDataMap: Map<String, com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockInterest>) {
        val targets = priceTargetRepository.findByAssetType(AssetType.STOCK)
        if (targets.isEmpty()) return

        val missingCodes = targets.map { it.assetCode }.filter { it !in stockDataMap }.distinct()
        val additionalMap = if (missingCodes.isNotEmpty()) {
            try {
                val stkCdParam = missingCodes.joinToString("|")
                val res = runBlocking { stockClient.stockInterest(KiwoomStockInterestReq(stk_cd = stkCdParam)) }
                res.atn_stk_infr?.associateBy { it.stk_cd ?: "" } ?: emptyMap()
            } catch (e: Exception) {
                log.warn { "목표가 주식 현재가 조회 실패: ${e.message}" }
                emptyMap()
            }
        } else emptyMap()

        val combinedMap = stockDataMap + additionalMap

        for (target in targets) {
            val stockData = combinedMap[target.assetCode] ?: continue
            val curPrc = abs(stockData.cur_prc?.toDoubleOrNull() ?: continue)

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
