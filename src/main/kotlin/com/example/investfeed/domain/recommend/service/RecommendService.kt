package com.example.investfeed.domain.recommend.service

import com.example.investfeed.common.util.DateUtil
import com.example.investfeed.domain.auth.repository.MemberRepository
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.holding.repository.MemberHoldingRepository
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.domain.recommend.dto.req.RecommendListStreamReq
import com.example.investfeed.domain.recommend.dto.res.RecommendListItem
import com.example.investfeed.domain.recommend.dto.res.RecommendListRes
import com.example.investfeed.domain.recommend.entity.MarketIndexSnapshot
import com.example.investfeed.domain.recommend.entity.RecommendSetting
import com.example.investfeed.domain.recommend.entity.RiskPreset
import com.example.investfeed.domain.recommend.entity.StockPick
import com.example.investfeed.domain.recommend.entity.StockPickHistory
import com.example.investfeed.domain.recommend.Position
import com.example.investfeed.domain.recommend.adjustment.AdjustmentModule
import com.example.investfeed.domain.recommend.marketmacro.MarketIndexAdjustmentModule
import com.example.investfeed.domain.recommend.marketmacro.MarketMacroCacheService
import com.example.investfeed.domain.recommend.marketmacro.MarketMacroSnapshot
import com.example.investfeed.domain.recommend.repository.MarketIndexSnapshotRepository
import com.example.investfeed.domain.recommend.repository.StockPickHistoryRepository
import com.example.investfeed.domain.recommend.repository.StockPickRepository
import com.example.investfeed.domain.stock.dto.req.StockInfoListReq
import com.example.investfeed.domain.stock.dto.res.StockInfoList
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.chart.client.StockChartClient
import com.example.investfeed.kiwoom.chart.dto.stock.req.KiwoomStockChartDayReq
import com.example.investfeed.kiwoom.price.client.PriceClient
import com.example.investfeed.kiwoom.price.dto.req.KiwoomInvestorTradeCloseMarketReq
import com.example.investfeed.kiwoom.price.dto.res.KiwoomInvestorTradeCloseMarketItemList
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockInvestor
import com.example.investfeed.kiwoom.stock.client.StockSocketClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomDefaultStockInfoReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInterestReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInvestorReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockStream
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockStreamReq
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.scheduling.annotation.Scheduled
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

@Service
class RecommendService(
    private val priceClient: PriceClient,
    private val stockClient: StockClient,
    private val stockChartClient: StockChartClient,
    private val stockSocketClient: StockSocketClient,
    private val stockPickRepository: StockPickRepository,
    private val stockPickHistoryRepository: StockPickHistoryRepository,
    private val memberRepository: MemberRepository,
    private val memberHoldingRepository: MemberHoldingRepository,
    private val brokerRepository: BrokerRepository,
    private val holidayService: HolidayService,
    private val authClient: AuthClient,
    private val schedulerLogService: SchedulerLogService,
    private val recommendSettingService: RecommendSettingService,
    private val marketIndexAdjustmentModule: MarketIndexAdjustmentModule,
    private val marketMacroCacheService: MarketMacroCacheService,
    private val marketIndexSnapshotRepository: MarketIndexSnapshotRepository,
    /**
     * 점수제 보정 모듈 목록. Spring 이 자동으로 모든 [AdjustmentModule] `@Component`
     * 구현체를 모아서 주입한다. 새 모듈 추가 시 본 클래스 수정 없이 모듈 클래스만 만들면 자동 합류.
     */
    private val adjustmentModules: List<AdjustmentModule>,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 22 * * *", scheduler = "slowScheduler")
    fun scheduledRecommendStock() {
        log.info { "RecommendScheduler cron fired" }
        if (holidayService.isHoliday()) {
            log.info { "RecommendScheduler skipped: today is holiday" }
            return
        }
        if (schedulerLogService.isRunning(SchedulerName.RecommendTodayDirectionScheduler)) {
            log.warn { "RecommendScheduler skipped: RecommendTodayDirectionScheduler 실행 중 (stock_pick 충돌 방지)" }
            return
        }
        runRecommendStock()
    }

    @Transactional
    fun runRecommendStock() {
        schedulerLogService.execute(SchedulerName.RecommendScheduler) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
                doRecommendStock()
            } finally {
                SecurityContextHolder.clearContext()
            }
        }
    }

    @Scheduled(cron = "0 */5 9-21 * * *", scheduler = "slowScheduler")
    fun scheduledRefreshTodayDirection() {
        log.info { "RecommendTodayDirectionScheduler cron fired" }
        if (holidayService.isHoliday()) {
            log.info { "RecommendTodayDirectionScheduler skipped: today is holiday" }
            return
        }
        if (schedulerLogService.isRunning(SchedulerName.RecommendScheduler)) {
            log.warn { "RecommendTodayDirectionScheduler skipped: RecommendScheduler 실행 중 (stock_pick 충돌 방지)" }
            return
        }
        runRefreshTodayDirection()
    }

    @Transactional
    fun runRefreshTodayDirection() {
        schedulerLogService.execute(SchedulerName.RecommendTodayDirectionScheduler) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
                doRefreshTodayDirection()
            } finally {
                SecurityContextHolder.clearContext()
            }
        }
    }

    private fun doRefreshTodayDirection() {
        val today = DateUtil.today("yyyyMMdd")
        val picks = stockPickRepository.findAll()
        if (picks.isEmpty()) return

        var matchCount = 0
        var mismatchCount = 0
        var nullCount = 0

        picks.forEach { pick ->
            try {
                Thread.sleep(API_PACING_MS)
                val res = stockClient.stockInvestor(
                    req = KiwoomStockInvestorReq(
                        dt = today,
                        stk_cd = pick.stkCd,
                        amt_qty_tp = "2",
                        trde_tp = "0",
                        unit_tp = "1"
                    )
                )
                if (res.return_code != 0) {
                    pick.todayDirection = null
                    nullCount++
                    return@forEach
                }
                val firstItem = res.stk_invsr_orgn?.firstOrNull()
                // 날짜 명시 비교: 오늘 데이터 없으면 null (휴일/자정/휴장일)
                if (firstItem?.dt != today) {
                    pick.todayDirection = null
                    nullCount++
                    return@forEach
                }
                val frgnr = firstItem.frgnr_invsr?.toLongOrNull() ?: 0L
                val penfnd = firstItem.penfnd_etc?.toLongOrNull() ?: 0L
                val direction = computeTodayDirection(frgnr, penfnd, pick.originSide)
                pick.todayDirection = direction
                when (direction) {
                    "MATCH" -> matchCount++
                    "MISMATCH" -> mismatchCount++
                    else -> nullCount++
                }
            } catch (e: Exception) {
                log.error(e) { "todayDirection 갱신 실패 stkCd=${pick.stkCd}" }
                pick.todayDirection = null
                nullCount++
            }
        }
        stockPickRepository.saveAll(picks)
        log.info { "당일 매매 동향 갱신: MATCH=$matchCount, MISMATCH=$mismatchCount, NULL=$nullCount" }
    }

    private fun computeTodayDirection(frgnr: Long, penfnd: Long, originSide: String?): String? {
        // MATCH: 외인+연기금 둘 다 추천 방향
        // MISMATCH: 외인+연기금 둘 다 추천 반대 방향 (강한 추세 전환 신호)
        // null: 한쪽만 반대거나 데이터 부족 (단기 노이즈 가능성 — 표시 안 함)
        return when (originSide) {
            "BUY" -> when {
                frgnr > 0 && penfnd > 0 -> "MATCH"
                frgnr < 0 && penfnd < 0 -> "MISMATCH"
                else -> null
            }
            "SELL" -> when {
                frgnr < 0 && penfnd < 0 -> "MATCH"
                frgnr > 0 && penfnd > 0 -> "MISMATCH"
                else -> null
            }
            else -> null
        }
    }

    private fun doRecommendStock() {
        val isHoliday = holidayService.isHoliday()
        val todayOffset = if (isHoliday) 0 else TODAY_OFFSET

        val now = if (isHoliday) {
            holidayService.lastTradingDay().atTime(22, 0)
        } else {
            LocalDateTime.now()
        }

        val (riskMap, marketTypeMap) = buildStockMetadataMaps()

        val kiwoomInvestorTradeCloseMarketRes = priceClient.investorTradeCloseMarket(
            req = KiwoomInvestorTradeCloseMarketReq(
                mrkt_tp = "000",
                amt_qty_tp = "1",
                trde_tp = "0",
                stex_tp = "3"
            )
        )

        val processed: List<ProcessedPick> = if (kiwoomInvestorTradeCloseMarketRes.return_code == 0) {
            val items = kiwoomInvestorTradeCloseMarketRes.opaf_invsr_trde ?: emptyList()
            val buyCandidates = extractIntersection(items, sortDesc = true)
            val sellCandidates = extractIntersection(items, sortDesc = false)

            log.info {
                "BUY 후보(${buyCandidates.size}): " +
                    buyCandidates.joinToString(", ") { "${it.stk_nm}(${it.stk_cd})" }
            }
            log.info {
                "SELL 후보(${sellCandidates.size}): " +
                    sellCandidates.joinToString(", ") { "${it.stk_nm}(${it.stk_cd})" }
            }

            buyCandidates.mapNotNull { processCandidate(it, Position.BUY, todayOffset, riskMap, marketTypeMap) } +
                sellCandidates.mapNotNull { processCandidate(it, Position.SELL, todayOffset, riskMap, marketTypeMap) }
        } else {
            emptyList()
        }

        // 현재용 테이블 갱신
        stockPickRepository.deleteAll()
        stockPickRepository.saveAll(processed.map { it.toCurrentEntity() })

        // 이력용 테이블 누적
        stockPickHistoryRepository.saveAll(processed.map { it.toHistoryEntity(now) })

        // 백테스트용 매크로 일일 스냅샷 저장 (당일 1행, 이미 있으면 skip).
        saveMarketIndexSnapshot(now)

        val counts = processed.groupingBy { it.type }.eachCount()
        log.info {
            "추천 분류 저장 완료 - " +
                "STRONG_BUY: ${counts["STRONG_BUY"] ?: 0}, " +
                "BUY: ${counts["BUY"] ?: 0}, " +
                "HOLD: ${counts["HOLD"] ?: 0}, " +
                "SELL: ${counts["SELL"] ?: 0}, " +
                "STRONG_SELL: ${counts["STRONG_SELL"] ?: 0}"
        }
    }

    /**
     * 22시 스케줄러 시점에 Redis 매크로 캐시 (16:00 polling 결과) 를 DB 에 1행 영속.
     * 백테스트/디버깅용 — 매크로 시나리오별 N일 후 수익률 분석 등 사후 추적 가능하게 함.
     *
     * 중복 호출 방지: captured_date UNIQUE — 같은 날 이미 있으면 skip.
     * 캐시 미스 (KOSPI/KOSDAQ 둘 다 없음) 시 저장 스킵.
     */
    private fun saveMarketIndexSnapshot(now: LocalDateTime) {
        val date = now.toLocalDate()
        marketIndexSnapshotRepository.findByCapturedDate(date)?.let {
            log.info { "MarketIndexSnapshot 이미 존재 ($date), 저장 스킵" }
            return
        }

        val kospi = marketMacroCacheService.getSnapshot("KOSPI")
        val kosdaq = marketMacroCacheService.getSnapshot("KOSDAQ")

        if (kospi == null && kosdaq == null) {
            log.warn { "매크로 캐시 미스 (KOSPI/KOSDAQ 둘 다 없음) — MarketIndexSnapshot 저장 스킵 ($date)" }
            return
        }

        marketIndexSnapshotRepository.save(
            MarketIndexSnapshot(
                capturedDate = date,
                kospiChangeRate = kospi?.priceChangeRate?.toDouble(),
                kospiForeignerSign = signOf(kospi?.foreignNetBuy),
                kospiInstitutionSign = signOf(kospi?.institutionalNetBuy),
                kospiScenario = scenarioOf(kospi),
                kosdaqChangeRate = kosdaq?.priceChangeRate?.toDouble(),
                kosdaqForeignerSign = signOf(kosdaq?.foreignNetBuy),
                kosdaqInstitutionSign = signOf(kosdaq?.institutionalNetBuy),
                kosdaqScenario = scenarioOf(kosdaq),
                capturedAt = now,
            )
        )
        log.info {
            "MarketIndexSnapshot 저장 완료 ($date) - " +
                "KOSPI=${scenarioOf(kospi) ?: "MISS"}, KOSDAQ=${scenarioOf(kosdaq) ?: "MISS"}"
        }
    }

    private fun signOf(value: Long?): String? = when {
        value == null -> null
        value > 0 -> "BUY"
        value < 0 -> "SELL"
        else -> "NEUTRAL"
    }

    /**
     * 매크로 6가지 케이스 + 중립 분류 (MarketIndexAdjustmentModule 의 조건문과 동일 의미).
     * 백테스트 SQL 에서 GROUP BY 키로 사용.
     */
    private fun scenarioOf(snap: MarketMacroSnapshot?): String? {
        if (snap == null) return null
        val isUp = snap.priceChangeRate.signum() > 0
        val isDown = snap.priceChangeRate.signum() < 0
        val instBuy = snap.institutionalNetBuy > 0
        val instSell = snap.institutionalNetBuy < 0
        val frgnBuy = snap.foreignNetBuy > 0
        val frgnSell = snap.foreignNetBuy < 0
        return when {
            isUp && instBuy && frgnBuy -> "UP_BUY_BUY"           // 케이스 1: BUY 격상
            isUp && instBuy && frgnSell -> "UP_BUY_SELL"          // 케이스 2: 다이버전스 유지
            isUp && instSell && frgnSell -> "UP_SELL_SELL"        // 케이스 3: BUY 격하
            isDown && instSell && frgnSell -> "DOWN_SELL_SELL"    // 케이스 4: SELL 격상
            isDown && instBuy && frgnSell -> "DOWN_BUY_SELL"      // 케이스 5: 다이버전스 유지
            isDown && instBuy && frgnBuy -> "DOWN_BUY_BUY"        // 케이스 6: SELL 격하
            else -> "NEUTRAL"
        }
    }

    private fun processCandidate(
        item: KiwoomInvestorTradeCloseMarketItemList,
        position: Position,
        todayOffset: Int = TODAY_OFFSET,
        riskMap: Map<String, RiskFlags> = emptyMap(),
        marketTypeMap: Map<String, String> = emptyMap(),
    ): ProcessedPick? {
        val stkCd = item.stk_cd ?: return null
        val stkNm = item.stk_nm ?: return null

        Thread.sleep(API_PACING_MS)

        val investorRes = try {
            stockClient.stockInvestor(
                req = KiwoomStockInvestorReq(
                    dt = DateUtil.today("yyyyMMdd"),
                    stk_cd = stkCd,
                    amt_qty_tp = "2",
                    trde_tp = "0",
                    unit_tp = "1"
                )
            )
        } catch (e: Exception) {
            log.error(e) { "stockInvestor 호출 실패 stkCd=$stkCd position=$position" }
            return null
        }

        if (investorRes.return_code != 0) {
            log.info { "[$stkNm($stkCd) $position] return_code=${investorRes.return_code} 컷" }
            return null
        }
        val items = investorRes.stk_invsr_orgn ?: run {
            log.info { "[$stkNm($stkCd) $position] stk_invsr_orgn null 컷" }
            return null
        }

        val window = items.take(todayOffset + RECENT_WINDOW + PRIOR_WINDOW)
        val penfndValues = window.map { it.penfnd_etc?.toLongOrNull() ?: 0L }
        val frgnrValues = window.map { it.frgnr_invsr?.toLongOrNull() ?: 0L }

        // 연기금 시그널 통과 못 하면 추천 풀에서 제외.
        if (!evaluateSignal(items, position, todayOffset)) {
            log.info {
                "[$stkNm($stkCd) $position] 시그널 미달 컷 — 연기금 시계열=$penfndValues"
            }
            return null
        }

        val penfndK = computeK(penfndValues, position, todayOffset)
        val frgnrBlocked = isForeignerBlocked(items, position, todayOffset)
        log.info {
            "[$stkNm($stkCd) $position] 시그널 통과 — penfndK=${"%.2f".format(penfndK)}, frgnrBlocked=$frgnrBlocked, " +
                "연기금=$penfndValues, 외국인=$frgnrValues"
        }
        val pickPrice = abs(items[0].cur_prc?.toLongOrNull() ?: 0L)

        val marketCap = try {
            stockClient.stockDefaultInfo(KiwoomDefaultStockInfoReq(stk_cd = stkCd)).mac?.toLongOrNull()
        } catch (e: Exception) {
            log.error(e) { "stockDefaultInfo 호출 실패 stkCd=$stkCd" }
            null
        }

        if (marketCap == null || marketCap == 0L) {
            log.error {
                "시총 데이터 누락으로 추천 후보 제외 stkCd=$stkCd, stkNm=$stkNm, position=$position, marketCap=$marketCap"
            }
            return null
        }

        val frgnrSignedRatio = computeForeignerSignedMcapRatio(window, marketCap, todayOffset)
        val effectiveRatio = effectiveForeignerRatio(frgnrSignedRatio, position)
        val prior = penfndValues.subList(todayOffset + RECENT_WINDOW, penfndValues.size)
        val priorTrendRatio = computeDominantStrengthRatio(prior)
        val foreignerAligned = isForeignerDirectionallyAligned(items, position, todayOffset)
        val type = classify(penfndK, frgnrBlocked, effectiveRatio, position, prior, foreignerAligned)
        log.info {
            "[$stkNm($stkCd) $position] 분류=$type — penfndK=${"%.2f".format(penfndK)} " +
                "(STRONG≥$K_STRONG_OVERRIDE), frgnrSignedRatio=${"%.4f%%".format(frgnrSignedRatio * 100)}, " +
                "effectiveRatio=${"%.4f%%".format(effectiveRatio * 100)} " +
                "(BUY≥${MCAP_RATIO_BUY * 100}%, STRONG≥${MCAP_RATIO_STRONG * 100}%), " +
                "priorTrendRatio=${"%.2f".format(priorTrendRatio)} (STRONG격상≥$TREND_CLARITY_THRESHOLD), " +
                "foreignerAligned=$foreignerAligned, marketCap=${marketCap}억"
        }

        val baseStkCd = stkCd.substringBefore("_")
        val riskFlags = riskMap[baseStkCd] ?: RiskFlags.UNKNOWN
        val priceMetrics = computePriceMetrics(stkCd)
        val marketType = marketTypeMap[baseStkCd]

        // 백테스트/디버깅용 — 모든 모듈 ON 가정의 trigger 결과 + 매크로 반영 최종 등급.
        // 사용자 응답 로직과 독립적 (사용자 옵션 따라 applyAdjustments 가 별도 재계산).
        val tempPick = StockPick(
            type = type, stkCd = stkCd, stkNm = stkNm,
            marketType = marketType,
            flu5Pct = priceMetrics.flu5Pct,
            ma5 = priceMetrics.ma5,
            ma20 = priceMetrics.ma20,
            avg20dVolume = priceMetrics.avg20dVolume,
            todayChangeRate = priceMetrics.todayChangeRate,
            todayVolume = priceMetrics.todayVolume,
            rsi14 = priceMetrics.rsi14,
            rsi14Breakdown70 = priceMetrics.rsi14Breakdown70,
            high52w = priceMetrics.high52w,
            low52w = priceMetrics.low52w,
            distFromHigh52w = priceMetrics.distFromHigh52w,
            distFromLow52w = priceMetrics.distFromLow52w,
            closeAboveMa20 = priceMetrics.closeAboveMa20,
        )
        val backtestMeta = evaluateBacktestMeta(tempPick)

        return ProcessedPick(
            type = type,
            stkCd = stkCd,
            stkNm = stkNm,
            marketType = marketType,
            penfndK = penfndK,
            frgnrBlocked = frgnrBlocked,
            frgnrMcapRatio = frgnrSignedRatio,
            pickPrice = pickPrice,
            marketCap = marketCap,
            originSide = position.name,
            riskFlags = riskFlags,
            priceMetrics = priceMetrics,
            backtestMeta = backtestMeta,
        )
    }

    private fun computePriceMetrics(stkCd: String): PriceMetrics {
        return try {
            Thread.sleep(API_PACING_MS)
            val res = stockChartClient.chartDayList(
                req = KiwoomStockChartDayReq(
                    stk_cd = stkCd,
                    base_dt = DateUtil.today("yyyyMMdd"),
                    upd_stkpc_tp = "1",
                )
            )
            if (res.return_code != 0) return PriceMetrics.UNKNOWN
            val rows = res.stk_dt_pole_chart_qry ?: return PriceMetrics.UNKNOWN
            val closes = rows.mapNotNull { it.cur_prc?.toLongOrNull() }.map { abs(it) }
            if (closes.isEmpty()) return PriceMetrics.UNKNOWN

            val flu5Pct = if (closes.size >= 5) {
                (closes[0] - closes[4]).toDouble() / closes[4] * 100
            } else null

            val ma5 = if (closes.size >= 5) closes.take(5).map { it.toDouble() }.average() else null
            val ma20 = if (closes.size >= 20) closes.take(20).map { it.toDouble() }.average() else null

            // VolumePriceModule 평가용 — 종목 당일 등락률 + 당일 거래량 + 20일 평균 거래량
            val todayChangeRate = if (closes.size >= 2 && closes[1] > 0) {
                (closes[0] - closes[1]).toDouble() / closes[1] * 100
            } else null

            val volumes = rows.mapNotNull { it.trde_qty?.toLongOrNull() }.map { abs(it) }
            val todayVolume = volumes.firstOrNull()
            // 20일 평균: 당일(idx 0) 제외하고 직전 20영업일 (idx 1~20)
            val avg20dVolume = if (volumes.size >= 21) {
                volumes.subList(1, 21).average().toLong()
            } else if (volumes.size >= 2) {
                // 데이터 부족 시 가능한 만큼 평균 (당일 제외)
                volumes.subList(1, volumes.size).average().toLong()
            } else null

            // RsiModule 평가용 — 최근 4일치 RSI 14 (오늘 + 어제 + 그제 + 3일 전).
            // Wilder smoothing 정확도 확보를 위해 RSI 한 점당 30일 가격 윈도 사용
            // (14일 초기 평균 + 15회 smoothing 누적으로 안정화).
            val rsiSeries = (0..3).mapNotNull { i ->
                val window = closes.drop(i).take(30)
                if (window.size >= 15) calculateRsi14(window) else null
            }
            val rsi14 = rsiSeries.firstOrNull()
            // 3일 전 RSI ≥ 70 + 최근 3일 모두 < 70 = "70 도달 후 3일 회복 못 함"
            val rsi14Breakdown70 = if (rsiSeries.size >= 4) {
                rsiSeries[3] >= 70.0 && rsiSeries.take(3).all { it < 70.0 }
            } else null

            // HighLow52wModule 평가용 — 240영업일 최고/최저 + 거리 % + MA20 위/아래.
            // closes.take(240) = 오늘 포함 직전 240영업일 윈도. 데이터 부족 시 null.
            val window52w = closes.take(240)
            val high52w = if (window52w.size >= 240) window52w.max() else null
            val low52w = if (window52w.size >= 240) window52w.min() else null
            val today = closes[0]
            val distFromHigh52w = high52w?.let { (today - it).toDouble() / it * 100 }
            val distFromLow52w = low52w?.let { (today - it).toDouble() / it * 100 }
            val closeAboveMa20 = ma20?.let { today.toDouble() > it }

            PriceMetrics(
                flu5Pct = flu5Pct,
                ma5 = ma5,
                ma20 = ma20,
                todayChangeRate = todayChangeRate,
                todayVolume = todayVolume,
                avg20dVolume = avg20dVolume,
                rsi14 = rsi14,
                rsi14Breakdown70 = rsi14Breakdown70,
                high52w = high52w,
                low52w = low52w,
                distFromHigh52w = distFromHigh52w,
                distFromLow52w = distFromLow52w,
                closeAboveMa20 = closeAboveMa20,
            )
        } catch (e: Exception) {
            log.error(e) { "가격 지표 계산 실패 stkCd=$stkCd" }
            PriceMetrics.UNKNOWN
        }
    }

    /**
     * Wilder smoothing 기반 14일 RSI 계산 (표준).
     *
     * @param closes 종가 리스트. Kiwoom 응답 기준 [당일, 어제, 그제, ...] 순서.
     *               최소 15개 이상 필요 (14일 변화량 = 15개 가격).
     */
    private fun calculateRsi14(closes: List<Long>): Double? {
        if (closes.size < 15) return null
        // [오래된 → 최근] 순으로 정렬해서 시간 순 계산
        val sorted = closes.map { it.toDouble() }.reversed()
        val changes = (1 until sorted.size).map { sorted[it] - sorted[it - 1] }
        val gains = changes.map { if (it > 0) it else 0.0 }
        val losses = changes.map { if (it < 0) -it else 0.0 }

        // 초기 14일 단순 평균
        var avgGain = gains.take(14).average()
        var avgLoss = losses.take(14).average()
        // 15일 이후 Wilder smoothing
        for (i in 14 until changes.size) {
            avgGain = (avgGain * 13 + gains[i]) / 14
            avgLoss = (avgLoss * 13 + losses[i]) / 14
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    internal data class PriceMetrics(
        val flu5Pct: Double?,
        val ma5: Double?,
        val ma20: Double?,
        val todayChangeRate: Double? = null,
        val todayVolume: Long? = null,
        val avg20dVolume: Long? = null,
        val rsi14: Double? = null,
        val rsi14Breakdown70: Boolean? = null,
        val high52w: Long? = null,
        val low52w: Long? = null,
        val distFromHigh52w: Double? = null,
        val distFromLow52w: Double? = null,
        val closeAboveMa20: Boolean? = null,
    ) {
        companion object {
            val UNKNOWN = PriceMetrics(
                flu5Pct = null, ma5 = null, ma20 = null,
            )
        }
    }

    private fun buildRiskCategoryMap(): Map<String, RiskFlags> {
        return try {
            val kospi = stockClient.stockInfoList(StockInfoListReq(mrkt_tp = "0")).list ?: emptyList()
            Thread.sleep(API_PACING_MS)
            val kosdaq = stockClient.stockInfoList(StockInfoListReq(mrkt_tp = "10")).list ?: emptyList()
            val combined = kospi + kosdaq
            val map = combined
                .filter { !it.code.isNullOrBlank() }
                .associate { it.code!! to RiskFlags.from(it) }
            log.info { "위험 카테고리 맵 구축 완료: kospi=${kospi.size}, kosdaq=${kosdaq.size}, distinct=${map.size}" }
            map
        } catch (e: Exception) {
            log.error(e) { "위험 카테고리 맵 구축 실패 - 빈 맵 반환" }
            emptyMap()
        }
    }

    private fun buildStockMetadataMaps(): Pair<Map<String, RiskFlags>, Map<String, String>> {
        return try {
            val kospi = stockClient.stockInfoList(StockInfoListReq(mrkt_tp = "0")).list ?: emptyList()
            Thread.sleep(API_PACING_MS)
            val kosdaq = stockClient.stockInfoList(StockInfoListReq(mrkt_tp = "10")).list ?: emptyList()
            val combined = kospi + kosdaq
            val riskMap = combined
                .filter { !it.code.isNullOrBlank() }
                .associate { it.code!! to RiskFlags.from(it) }
            val marketTypeMap =
                kospi.filter { !it.code.isNullOrBlank() }.associate { it.code!! to "KOSPI" } +
                    kosdaq.filter { !it.code.isNullOrBlank() }.associate { it.code!! to "KOSDAQ" }
            log.info {
                "종목 메타데이터 맵 구축 완료: kospi=${kospi.size}, kosdaq=${kosdaq.size}, " +
                    "riskDistinct=${riskMap.size}, marketTypeDistinct=${marketTypeMap.size}"
            }
            riskMap to marketTypeMap
        } catch (e: Exception) {
            log.error(e) { "종목 메타데이터 맵 구축 실패 - 빈 맵 반환" }
            emptyMap<String, RiskFlags>() to emptyMap()
        }
    }

    internal data class RiskFlags(
        val isManaged: Boolean?,
        val isDelisting: Boolean?,
        val isOverheated: Boolean?,
        val isInvestmentRisk: Boolean?,
        val isInvestmentWarning: Boolean?,
        val isInvestorAlert: Boolean?,
        val isTradingHalted: Boolean?,
    ) {
        companion object {
            /** 위험 카테고리 정보 미확보 — 모든 플래그 null. */
            val UNKNOWN = RiskFlags(null, null, null, null, null, null, null)

            fun from(item: StockInfoList): RiskFlags {
                val stateValues = (item.state ?: "").split("|").map { it.trim() }
                val ow = item.orderWarning
                return RiskFlags(
                    isManaged = stateValues.contains("관리종목"),
                    isDelisting = ow == "2",
                    isOverheated = ow == "3",
                    isInvestmentRisk = ow == "4",
                    isInvestmentWarning = ow == "5",
                    isInvestorAlert = item.auditInfo == "투자주의환기종목",
                    isTradingHalted = item.auditInfo == "거래정지",
                )
            }
        }
    }

    /**
     * prior 10일 매수/매도 강도 비율 — 한 방향이 전체 강도의 몇 %를 차지하는지.
     *
     * 횡보 판정 (`< TREND_CLARITY_THRESHOLD`)에 사용. 일수가 아닌 **강도** 기반이라
     * 5일×+5000 + 5일×-500 같은 "일수 비등하지만 강도 한쪽 압도적" 케이스를
     * 추세 명확으로 인정 (false positive 방지).
     */
    internal fun computeDominantStrengthRatio(values: List<Long>): Double {
        val buyTotal = values.filter { it > 0 }.sumOf { it.toDouble() }
        val sellTotal = abs(values.filter { it < 0 }.sumOf { it.toDouble() })
        val total = buyTotal + sellTotal
        if (total == 0.0) return 0.5  // 데이터 없음 = 중립
        return maxOf(buyTotal, sellTotal) / total
    }

    /**
     * 외국인 방향성 동조 검사 — "외국인이 12일 동안 추천 방향과 같은 방향으로 일관 매매했는가"
     *
     * 시총 비중 임계 미달 종목(예: 두산 같은 시총 큰 지주사)을 구제하기 위한 보조 룰.
     * 외국인 거래량은 작아도 prior 10일 추세가 명확(≥ 70%)하고 recent 2일도 그 방향 유지면
     * "조용한 매도/매수 추세" 시그널로 인정.
     *
     * 3가지 조건 모두 충족 시 true:
     *   ① 외국인 prior 10일 magnitude ratio ≥ 70% (한 방향 일관)
     *   ② 외국인 prior 10일 sum 방향 = 추천 방향
     *   ③ 외국인 recent 2일 sum 방향 = 추천 방향
     */
    internal fun isForeignerDirectionallyAligned(
        items: List<KiwoomStockInvestor>,
        position: Position,
        todayOffset: Int = TODAY_OFFSET,
    ): Boolean {
        val window = items.take(todayOffset + RECENT_WINDOW + PRIOR_WINDOW)
        if (window.size <= todayOffset + RECENT_WINDOW) return false
        val frgnr = window.map { it.frgnr_invsr?.toLongOrNull() ?: 0L }
        val recent = frgnr.subList(todayOffset, todayOffset + RECENT_WINDOW)
        val prior = frgnr.subList(todayOffset + RECENT_WINDOW, frgnr.size)
        if (prior.isEmpty()) return false

        // 조건 ①: prior 10일 추세 명확 (B' 와 동일 임계 재사용)
        val priorRatio = computeDominantStrengthRatio(prior)
        if (priorRatio < TREND_CLARITY_THRESHOLD) return false

        // 조건 ②③: prior + recent 둘 다 추천 방향 일치
        val priorSum = prior.sum()
        val recentSum = recent.sum()
        return when (position) {
            Position.SELL -> priorSum < 0 && recentSum < 0
            Position.BUY -> priorSum > 0 && recentSum > 0
        }
    }

    /**
     * 분류 규칙 우선순위:
     * 1. 외국인 BLOCK → HOLD (외국인이 추천 반대 방향으로 강한 시그널)
     * 2. STRONG 격상 (연기금 K ≥ 3.0 또는 외국인 시총 비중 ≥ 0.1%) + prior 추세 명확(B' ≥ 70%) → STRONG
     * 3. 외국인 시총 비중 ≥ MCAP_RATIO_BUY (0.05%) → BUY/SELL
     * 4. **(옵션 B)** 외국인 방향성 동조 (12일 추세 일관) → BUY/SELL ← 신규: 시총 비중 미달 구제
     * 5. 그 외 → HOLD
     *
     * @param foreignerAligned [isForeignerDirectionallyAligned] 결과. 호출부에서 미리 계산해서 전달.
     */
    internal fun classify(
        penfndK: Double,
        frgnrBlocked: Boolean,
        foreignerEffectiveRatio: Double,
        position: Position,
        prior: List<Long>,
        foreignerAligned: Boolean = false,
    ): String {
        if (frgnrBlocked) return "HOLD"
        val sideName = position.name

        val priorTrendUnclear = computeDominantStrengthRatio(prior) < TREND_CLARITY_THRESHOLD

        // STRONG 격상은 prior 추세 명확할 때만 (모호하면 1단계 격하)
        if (penfndK >= K_STRONG_OVERRIDE && !priorTrendUnclear) return "STRONG_$sideName"
        return when {
            foreignerEffectiveRatio >= MCAP_RATIO_STRONG && !priorTrendUnclear -> "STRONG_$sideName"
            foreignerEffectiveRatio >= MCAP_RATIO_BUY -> sideName  // BUY/SELL은 격하 영향 없음
            foreignerAligned -> sideName  // 옵션 B: 시총 비중 미달이지만 외국인 12일 일관 추세
            else -> "HOLD"
        }
    }

    /**
     * 연기금 K값 계산 (평균/평균 비교).
     * BUY: recent 일평균 매수 / prior 일평균 매도
     * SELL: recent 일평균 매도 / prior 일평균 매수
     * 의미: K = "매수 강도가 매도 강도의 K배" (또는 그 반대).
     *
     * @param todayOffset 평일 정규 실행 시 1 (idx 0 제외), 휴일 force 트리거 시 0 (idx 0 = 마지막 거래일 포함).
     */
    internal fun computeK(values: List<Long>, position: Position, todayOffset: Int = TODAY_OFFSET): Double {
        if (values.size <= todayOffset + RECENT_WINDOW) return 0.0
        val recent = values.subList(todayOffset, todayOffset + RECENT_WINDOW)
        val prior = values.subList(todayOffset + RECENT_WINDOW, values.size)
        if (prior.isEmpty()) return 0.0
        val priorAvg = prior.sum().toDouble() / prior.size
        val recentAvg = recent.sum().toDouble() / recent.size
        return when (position) {
            Position.BUY -> if (priorAvg < 0) recentAvg / -priorAvg else 0.0
            Position.SELL -> if (priorAvg > 0) -recentAvg / priorAvg else 0.0
        }
    }

    /**
     * recent 2일 외국인 순매수 금액(원) ÷ 시가총액(원).
     *
     * 키움 ka10001의 mac 필드는 **억원 단위**라 원으로 환산해서 분모로 사용.
     * 결과 부호: 양수=매수 방향, 음수=매도 방향.
     *
     * @param todayOffset 평일=1 (idx 0 제외), 휴일 force=0 (idx 0 포함).
     */
    private fun computeForeignerSignedMcapRatio(
        window: List<KiwoomStockInvestor>,
        marketCap: Long,
        todayOffset: Int = TODAY_OFFSET,
    ): Double {
        if (marketCap <= 0L) return 0.0
        val recent = if (window.size > todayOffset) {
            window.subList(todayOffset, minOf(todayOffset + RECENT_WINDOW, window.size))
        } else emptyList()
        val signedAmount = recent.sumOf {
            val qty = it.frgnr_invsr?.toLongOrNull() ?: 0L
            val price = abs(it.cur_prc?.toLongOrNull() ?: 0L)
            qty * price
        }
        val marketCapWon = marketCap * MARKET_CAP_UNIT_WON
        return signedAmount.toDouble() / marketCapWon
    }

    /** 분류용 effective ratio: 추천 방향과 같은 부호일 때 절대값, 반대 부호면 0. */
    private fun effectiveForeignerRatio(signedRatio: Double, position: Position): Double = when (position) {
        Position.BUY -> if (signedRatio > 0) signedRatio else 0.0
        Position.SELL -> if (signedRatio < 0) -signedRatio else 0.0
    }

    private data class ProcessedPick(
        val type: String,
        val stkCd: String,
        val stkNm: String,
        val marketType: String?,
        val penfndK: Double,
        val frgnrBlocked: Boolean,
        val frgnrMcapRatio: Double?,
        val pickPrice: Long,
        val marketCap: Long?,
        val originSide: String,
        val riskFlags: RiskFlags,
        val priceMetrics: PriceMetrics,
        val backtestMeta: BacktestMeta,
    ) {
        fun toCurrentEntity(): StockPick = StockPick(
            type = type,
            stkCd = stkCd,
            stkNm = stkNm,
            marketType = marketType,
            penfndK = penfndK,
            frgnrBlocked = frgnrBlocked,
            frgnrMcapRatio = frgnrMcapRatio,
            originSide = originSide,
            isManaged = riskFlags.isManaged,
            isDelisting = riskFlags.isDelisting,
            isOverheated = riskFlags.isOverheated,
            isInvestmentRisk = riskFlags.isInvestmentRisk,
            isInvestmentWarning = riskFlags.isInvestmentWarning,
            isInvestorAlert = riskFlags.isInvestorAlert,
            isTradingHalted = riskFlags.isTradingHalted,
            flu5Pct = priceMetrics.flu5Pct,
            ma5 = priceMetrics.ma5,
            ma20 = priceMetrics.ma20,
            avg20dVolume = priceMetrics.avg20dVolume,
            todayChangeRate = priceMetrics.todayChangeRate,
            todayVolume = priceMetrics.todayVolume,
            rsi14 = priceMetrics.rsi14,
            rsi14Breakdown70 = priceMetrics.rsi14Breakdown70,
            high52w = priceMetrics.high52w,
            low52w = priceMetrics.low52w,
            distFromHigh52w = priceMetrics.distFromHigh52w,
            distFromLow52w = priceMetrics.distFromLow52w,
            closeAboveMa20 = priceMetrics.closeAboveMa20,
            pvTrigger = backtestMeta.pvTrigger,
            maTrigger = backtestMeta.maTrigger,
            vpTrigger = backtestMeta.vpTrigger,
            rsiTrigger = backtestMeta.rsiTrigger,
        )

        fun toHistoryEntity(pickDate: LocalDateTime): StockPickHistory = StockPickHistory(
            type = type,
            stkCd = stkCd,
            stkNm = stkNm,
            marketType = marketType,
            penfndK = penfndK,
            frgnrBlocked = frgnrBlocked,
            frgnrMcapRatio = frgnrMcapRatio,
            pickPrice = pickPrice,
            marketCap = marketCap,
            originSide = originSide,
            isManaged = riskFlags.isManaged,
            isDelisting = riskFlags.isDelisting,
            isOverheated = riskFlags.isOverheated,
            isInvestmentRisk = riskFlags.isInvestmentRisk,
            isInvestmentWarning = riskFlags.isInvestmentWarning,
            isInvestorAlert = riskFlags.isInvestorAlert,
            isTradingHalted = riskFlags.isTradingHalted,
            flu5Pct = priceMetrics.flu5Pct,
            ma5 = priceMetrics.ma5,
            ma20 = priceMetrics.ma20,
            avg20dVolume = priceMetrics.avg20dVolume,
            todayChangeRate = priceMetrics.todayChangeRate,
            todayVolume = priceMetrics.todayVolume,
            rsi14 = priceMetrics.rsi14,
            rsi14Breakdown70 = priceMetrics.rsi14Breakdown70,
            high52w = priceMetrics.high52w,
            low52w = priceMetrics.low52w,
            distFromHigh52w = priceMetrics.distFromHigh52w,
            distFromLow52w = priceMetrics.distFromLow52w,
            closeAboveMa20 = priceMetrics.closeAboveMa20,
            pvTrigger = backtestMeta.pvTrigger,
            maTrigger = backtestMeta.maTrigger,
            vpTrigger = backtestMeta.vpTrigger,
            rsiTrigger = backtestMeta.rsiTrigger,
            pickDate = pickDate,
        )
    }

    /**
     * 백테스트/디버깅용 메타데이터 — 후행지표 모듈의 trigger 결과만.
     *
     * 22:00 스케줄러가 모든 **후행** 모듈 ON 가정으로 1회 계산해 영속.
     * 사용자 응답 로직 [applyAdjustments] 와 독립.
     *
     * **매크로(동행지표)는 의도적으로 제외**:
     *  - 매크로 = 사용 시점의 시장 상황 반영이 본질
     *  - 22시 시점에 적용해 저장하면 시간 lag (T일 마감 매크로 → T+1일 매수)
     *  - 동행지표가 후행지표로 변질 → 백테스트 가정 부정확
     *  - 운영 시 [applyAdjustments] 가 실시간 매크로 적용 (DB 저장 X)
     *  - 매크로 환경 영향은 market_index_snapshot 분해로 측정
     */
    private data class BacktestMeta(
        val pvTrigger: String?,
        val maTrigger: String?,
        val vpTrigger: String?,
        val rsiTrigger: String?,
    ) {
        companion object {
            val EMPTY = BacktestMeta("NONE", "NONE", "NONE", "NONE")
        }
    }

    private fun extractIntersection(
        items: List<KiwoomInvestorTradeCloseMarketItemList>,
        sortDesc: Boolean
    ): List<KiwoomInvestorTradeCloseMarketItemList> {
        val frgnrSorted = if (sortDesc) {
            items.sortedByDescending { it.frgnr_invsr?.toLongOrNull() ?: 0L }
        } else {
            items.sortedBy { it.frgnr_invsr?.toLongOrNull() ?: 0L }
        }
        val penfndSorted = if (sortDesc) {
            items.sortedByDescending { it.penfnd_etc?.toLongOrNull() ?: 0L }
        } else {
            items.sortedBy { it.penfnd_etc?.toLongOrNull() ?: 0L }
        }

        val frgnrTopCodes = frgnrSorted.take(TOP_N).mapNotNull { it.stk_cd }.toSet()
        return penfndSorted.take(TOP_N).filter { it.stk_cd in frgnrTopCodes }
    }

    /**
     * 연기금 [position] 시그널(K_SIGNAL + 부호 일관성) 만족 여부.
     * 외국인 BLOCK 검사는 별도 단계에서 수행 (분류 시 BLOCK이면 HOLD로 분류).
     *
     * @param todayOffset 평일=1 (idx 0 in-progress 제외), 휴일 force=0 (idx 0 = 마지막 거래일 포함).
     */
    internal fun evaluateSignal(
        items: List<KiwoomStockInvestor>,
        position: Position,
        todayOffset: Int = TODAY_OFFSET,
    ): Boolean {
        val window = items.take(todayOffset + RECENT_WINDOW + PRIOR_WINDOW)
        val penfnd = window.map { it.penfnd_etc?.toLongOrNull() ?: 0L }
        return evaluateColumn(penfnd, position, K_SIGNAL, requireSignConsistency = true, todayOffset = todayOffset)
    }

    /**
     * 외국인이 추천 방향과 반대로 강한 시그널(K_BLOCK + 부호 일관성)이면 차단.
     *
     * @param todayOffset 평일=1, 휴일 force=0.
     */
    internal fun isForeignerBlocked(
        items: List<KiwoomStockInvestor>,
        position: Position,
        todayOffset: Int = TODAY_OFFSET,
    ): Boolean {
        val window = items.take(todayOffset + RECENT_WINDOW + PRIOR_WINDOW)
        val frgnr = window.map { it.frgnr_invsr?.toLongOrNull() ?: 0L }
        val opposite = if (position == Position.BUY) Position.SELL else Position.BUY
        return evaluateColumn(frgnr, opposite, K_BLOCK, requireSignConsistency = true, todayOffset = todayOffset)
    }

    /**
     * 평균/평균 비교 식. K = "매수 강도가 매도 강도의 K배" (또는 매도 강도가 매수의 K배).
     *
     * 인덱스 구조 (todayOffset=1 평일 기본):
     * - idx 0 (당일, 평가 제외)
     * - idx 1 ~ idx 2 = recent (어제 + 엊그제)
     * - idx 3 ~ end = prior 10일
     *
     * 휴일 force 트리거(todayOffset=0):
     * - idx 0 ~ idx 1 = recent (마지막 거래일 + 그 직전 거래일, 모두 완전 집계됨)
     * - idx 2 ~ end = prior 10일
     *
     * BUY: 이전 N일 누적 순매도 + 최근 2일 누적 순매수가 일평균 매도분의 [k]배 이상
     * SELL: 이전 N일 누적 순매수 + 최근 2일 누적 순매도가 일평균 매수분의 [k]배 이상
     * [requireSignConsistency]가 true면 recent 2일 부호가 모두 같은 방향이어야 함 (단발 노이즈 컷).
     */
    internal fun evaluateColumn(
        values: List<Long>,
        position: Position,
        k: Double,
        requireSignConsistency: Boolean,
        todayOffset: Int = TODAY_OFFSET,
    ): Boolean {
        if (values.size <= todayOffset + RECENT_WINDOW) return false

        val recent = values.subList(todayOffset, todayOffset + RECENT_WINDOW)
        val prior = values.subList(todayOffset + RECENT_WINDOW, values.size)
        if (prior.isEmpty()) return false

        // signed sum: 양수면 매수 우세, 음수면 매도 우세
        val priorNetBuy = prior.sum()
        val recentNetBuy = recent.sum()

        return when (position) {
            Position.BUY -> {
                if (priorNetBuy >= 0 || recentNetBuy <= 0) return false
                if (requireSignConsistency && recent.any { it < 0 }) return false
                val priorAvgSell = -priorNetBuy.toDouble() / prior.size  // prior 일평균 매도 (양수)
                val recentAvgBuy = recentNetBuy.toDouble() / recent.size // recent 일평균 매수 (양수)
                recentAvgBuy >= priorAvgSell * k
            }
            Position.SELL -> {
                if (priorNetBuy <= 0 || recentNetBuy >= 0) return false
                if (requireSignConsistency && recent.any { it > 0 }) return false
                val priorAvgBuy = priorNetBuy.toDouble() / prior.size      // prior 일평균 매수 (양수)
                val recentAvgSell = -recentNetBuy.toDouble() / recent.size // recent 일평균 매도 (양수)
                recentAvgSell >= priorAvgBuy * k
            }
        }
    }

    companion object {
        private const val TOP_N = 100
        private const val TODAY_OFFSET = 1   // idx 0 = 당일 (평가 제외, 보조 지표용)
        private const val RECENT_WINDOW = 2  // idx 1+2 = 어제+엊그제
        private const val PRIOR_WINDOW = 10  // idx 3~12
        private const val K_SIGNAL = 1.5          // 연기금 시그널 통과 임계 (평균/평균)
        private const val K_BLOCK = 1.5           // 외국인 차단 임계 (평균/평균)
        private const val K_STRONG_OVERRIDE = 3.0 // 연기금 STRONG 격상 임계 (평균/평균)
        private const val TREND_CLARITY_THRESHOLD = 0.7 // prior 강도 비율 임계 (미만이면 STRONG 격하)
        private const val MCAP_RATIO_BUY = 0.0005    // 0.05%
        private const val MCAP_RATIO_STRONG = 0.001  // 0.1%
        private const val MARKET_CAP_UNIT_WON = 100_000_000L  // 키움 ka10001 mac 필드: 억원 단위
        private const val API_PACING_MS = 500L
        private const val KIWOOM_BROKER_NAME = "키움증권"
        private const val STREAK_LOOKBACK_DAYS = 45L      // history / 운영 사이클 조회 기간
    }

    fun listRecommendations(): RecommendListRes {
        val allPicks = stockPickRepository.findAll()
        val filteredPicks = applyRiskFilter(allPicks)
        val setting = resolveCurrentSetting()
        val effectiveTypes = computeEffectiveTypes(filteredPicks, setting)

        fun typeOf(pick: StockPick): String = effectiveTypes[pick.stkCd] ?: pick.type

        val holdingCodes = loadHoldingStkCds()
        val historyByStkCd = loadRecentHistoryByStkCd(filteredPicks.map { it.stkCd })
        val operatingDates = loadOperatingDates()

        fun toItem(entity: StockPick): RecommendListItem {
            val effectiveType = typeOf(entity)
            return RecommendListItem(
                type = effectiveType,
                stkCd = entity.stkCd,
                stkNm = entity.stkNm,
                // HOLD 는 매매 추천 X 라 당일 매매 동향 정보는 사용자에게 노이즈 → 마스킹
                todayDirection = if (effectiveType == "HOLD") null else entity.todayDirection,
                isHolding = entity.stkCd in holdingCodes,
                streakDays = computeStreakDays(
                    histories = historyByStkCd[entity.stkCd] ?: emptyList(),
                    currentType = effectiveType,
                    operatingDates = operatingDates,
                ),
            )
        }

        val recommendList = filteredPicks.filter { typeOf(it) == "STRONG_BUY" || typeOf(it) == "BUY" }
            .map(::toItem).toMutableList()
        val avoidList = filteredPicks.filter { typeOf(it) == "STRONG_SELL" || typeOf(it) == "SELL" }
            .map(::toItem).toMutableList()
        val holdList = filteredPicks.filter { typeOf(it) == "HOLD" }
            .map(::toItem).toMutableList()

        val allItems = recommendList + avoidList + holdList
        val allCodes = allItems.mapNotNull { it.stkCd }.joinToString("|")
        if (allCodes.isNotBlank()) {
            val kiwoomStockInterestRes = stockClient.stockInterest(req = KiwoomStockInterestReq(stk_cd = allCodes))
            if (kiwoomStockInterestRes.return_code == 0) {
                val infoMap = kiwoomStockInterestRes.atn_stk_infr?.associateBy { it.stk_cd } ?: emptyMap()
                allItems.forEach { item ->
                    infoMap[item.stkCd]?.let { info ->
                        item.curPrc = info.cur_prc
                        item.fluRt = info.flu_rt
                        item.preSig = info.pred_pre_sig
                        item.predPre = info.pred_pre
                    }
                }
            }
        }

        return RecommendListRes(recommendList = recommendList, avoidList = avoidList, holdList = holdList)
    }

    /**
     * 로그인 사용자의 RiskPreset 에 따라 위험 카테고리 종목을 제외한다.
     * - 거래정지 종목은 preset 무관 항상 제외 (매매 자체 불가능)
     * - preset 별 추가 제외 카테고리는 [RiskPreset.blockedCategories] 참조
     * - 비로그인/preference 미설정 사용자는 NORMAL 적용
     */
    private fun applyRiskFilter(picks: List<StockPick>): List<StockPick> {
        val preset = resolveCurrentSetting().riskPreset
        val blocked = preset.blockedCategories()
        return picks.filterNot { pick ->
            pick.isTradingHalted == true ||
                blocked.any { category -> category.matches(pick) }
        }
    }

    /**
     * 사용자 옵션 ON 인 보정 모듈들을 다수결 점수제로 평가해 effectiveType 결정.
     *
     * 점수 = (격상 트리거 개수) - (격하 트리거 개수)
     *   score ≥ 1   → 한 단계 격상 (BUY → STRONG_BUY, SELL → STRONG_SELL)
     *   score 0 + 격하 1+ → 한 단계 격하 (동률 시 격하 우선)
     *   score -1 ~ -2 → 한 단계 격하 (STRONG → 일반)
     *   score ≤ -3  → 두 단계 격하 (HOLD까지, 강한 모순 시그널)
     *
     * 룰:
     *   1. 수급 분류는 백본 — 분류 방향 (BUY ↔ SELL) 절대 못 뒤집음
     *   2. 격상은 최대 한 단계 (수익 추구는 신중)
     *   3. 격하는 보정 강도 따라 1~2단계 (손실 보호는 강하게)
     */
    private fun computeEffectiveTypes(
        picks: List<StockPick>,
        setting: RecommendSetting,
    ): Map<String, String> {
        return picks.associate { pick ->
            pick.stkCd to applyAdjustments(pick, setting)
        }
    }

    /**
     * 후행지표 보정 모듈은 **만장일치 룰** 로 평가 (사용자 철학 "잃지 않는 게 우선" 반영).
     *
     * 룰:
     * - 격상: 활성 격상 능력 모듈 모두 격상 트리거 + 격하 시그널 0 → 한 단계 격상
     * - 격하: 활성 격하 능력 모듈 모두 격하 트리거 + 격상 시그널 0 → 한 단계 격하
     * - 그 외 (혼합/단일 무보정/균형) → 유지
     * - 두 단계 격하는 폐기 (공격적 격하 회피)
     *
     * 단일 옵션 활성화 시 → 활성 능력 모듈 1개 → 1/1 = 만장일치 → 단독 발동 OK.
     *
     * 매크로(동행지표)는 점수제 밖에서 별도 한 단계 보정 추가 — 모듈 6가지 케이스 자체가
     * 내부 만장일치(지수 등락률 + 기관 + 외국인 합의) 메커니즘이라 단독 발동 정당.
     */
    private fun applyAdjustments(pick: StockPick, setting: RecommendSetting): String {
        val side = when (pick.type) {
            "STRONG_BUY", "BUY" -> Position.BUY
            "STRONG_SELL", "SELL" -> Position.SELL
            else -> return pick.type  // HOLD는 보정 대상 X
        }

        val activeModules = activeModules(setting)
        val afterScoring = if (activeModules.isEmpty()) {
            pick.type
        } else {
            val promotableModules = activeModules.filter { it.canPromote(side) }
            val demotableModules = activeModules.filter { it.canDemote(side) }

            val promotionCount = activeModules.count { it.shouldPromote(pick, side) }
            val demotionCount = activeModules.count { it.shouldDemote(pick, side) }

            val isUnanimousPromote = promotableModules.isNotEmpty() &&
                promotableModules.all { it.shouldPromote(pick, side) } &&
                demotionCount == 0

            val isUnanimousDemote = demotableModules.isNotEmpty() &&
                demotableModules.all { it.shouldDemote(pick, side) } &&
                promotionCount == 0

            when {
                isUnanimousPromote -> promoteOnce(pick.type)
                isUnanimousDemote -> demoteOnce(pick.type)
                else -> pick.type
            }
        }

        // 2단계: 매크로 보정 (동행지표, 별도 처리). 캐시 미스 시 무보정.
        return if (setting.marketIndexEnabled) {
            marketIndexAdjustmentModule.adjust(afterScoring, pick)
        } else {
            afterScoring
        }
    }

    /**
     * 사용자 옵션 ON 상태인 보정 모듈만 필터링. 새 모듈은 `@Component` 로 만들면
     * [adjustmentModules] 에 자동 합류하므로 본 메서드 수정 불필요.
     */
    private fun activeModules(setting: RecommendSetting): List<AdjustmentModule> {
        return adjustmentModules.filter { it.isEnabled(setting) }
    }

    /**
     * 백테스트/디버깅용 — 22:00 스케줄러 시점에 **모든 후행 모듈 ON** 가정으로
     * 각 모듈의 raw trigger 결과만 저장 (만장일치 룰 적용 전).
     *
     * 매크로(동행지표)는 의도적으로 제외 — 시간 lag 시 의미 변질.
     * 매크로 보정은 [applyAdjustments] 가 사용자 응답 시점에 실시간 적용 (DB 저장 X).
     *
     * HOLD 종목 = 보정 대상 X → 모든 trigger NONE.
     */
    private fun evaluateBacktestMeta(pick: StockPick): BacktestMeta {
        val side = when (pick.type) {
            "STRONG_BUY", "BUY" -> Position.BUY
            "STRONG_SELL", "SELL" -> Position.SELL
            else -> return BacktestMeta.EMPTY
        }

        val triggers = adjustmentModules.associate { module ->
            val result = when {
                module.shouldPromote(pick, side) -> "PROMOTE"
                module.shouldDemote(pick, side) -> "DEMOTE"
                else -> "NONE"
            }
            module.name to result
        }

        return BacktestMeta(
            pvTrigger = triggers["PriceVolatility"] ?: "NONE",
            maTrigger = triggers["MovingAverage"] ?: "NONE",
            vpTrigger = triggers["VolumePrice"] ?: "NONE",
            rsiTrigger = triggers["Rsi"] ?: "NONE",
        )
    }

    private fun promoteOnce(type: String): String = when (type) {
        "BUY" -> "STRONG_BUY"
        "SELL" -> "STRONG_SELL"
        else -> type  // STRONG_BUY/STRONG_SELL은 이미 STRONG — 격상 안 함
    }

    private fun demoteOnce(type: String): String = when (type) {
        "STRONG_BUY" -> "BUY"
        "BUY" -> "HOLD"            // 한 단계 격하 = 등급 한 칸 다운
        "STRONG_SELL" -> "SELL"
        "SELL" -> "HOLD"
        else -> type
    }

    private fun resolveCurrentSetting(): RecommendSetting {
        val default = RecommendSetting(memberId = 0L)
        val loginId = currentLoginIdOrNull() ?: return default
        val member = memberRepository.findByLoginId(loginId).orElse(null) ?: return default
        return recommendSettingService.getSettingByMemberIdOrDefault(member.id)
    }

    private fun loadHoldingStkCds(): Set<String> {
        val loginId = currentLoginIdOrNull() ?: return emptySet()
        val member = memberRepository.findByLoginId(loginId).orElse(null) ?: return emptySet()
        val broker = brokerRepository.findByName(KIWOOM_BROKER_NAME) ?: return emptySet()
        return memberHoldingRepository
            .findByMemberIdAndBrokerIdOrderByDisplayOrderAsc(member.id, broker.id)
            .map { it.stkCd }
            .toSet()
    }

    /** stkCd 별 최근 STREAK_LOOKBACK_DAYS 일 history (pickDate desc). 빈 입력이면 emptyMap. */
    private fun loadRecentHistoryByStkCd(stkCds: List<String>): Map<String, List<StockPickHistory>> {
        if (stkCds.isEmpty()) return emptyMap()
        val after = LocalDateTime.now().minusDays(STREAK_LOOKBACK_DAYS)
        return stockPickHistoryRepository
            .findByStkCdInAndPickDateAfterOrderByStkCdAscPickDateDesc(stkCds, after)
            .groupBy { it.stkCd }
    }

    /**
     * 추천 결과가 실제로 저장된 운영일 집합 (desc).
     *
     * stock_pick_history 의 distinct pickDate (date 단위) 를 사용한다.
     * - 수동 트리거든 정상 cron 이든 history 에 row 가 만들어진 날만 운영일로 인정 → streak 계산 일관
     * - 휴장 수동 force 트리거가 빈 결과로 끝난 날은 history row 가 없어 자연 제외 → false positive 방지
     * - 휴일 force 모드에서 pickDate 가 직전 거래일 22:00 으로 들어가도 동일 기준으로 처리됨
     */
    private fun loadOperatingDates(): List<LocalDate> {
        val after = LocalDateTime.now().minusDays(STREAK_LOOKBACK_DAYS)
        return stockPickHistoryRepository
            .findDistinctPickDatesAfter(after)
            .distinct()
            .sortedDescending()
    }

    private fun currentLoginIdOrNull(): String? {
        val auth = SecurityContextHolder.getContext().authentication ?: return null
        if (!auth.isAuthenticated) return null
        if (auth.name == "anonymousUser") return null
        return auth.name
    }

    /**
     * 같은 진영(매수: STRONG_BUY+BUY / 매도: STRONG_SELL+SELL)으로 연속 추천된 일수 계산.
     *
     * 알고리즘: 최근 운영일부터 거꾸로 훑으며
     * - 그 운영일에 이 종목 row 가 없거나(추천 풀에서 빠진 날) → 단절
     * - row 가 있어도 진영이 다르면(HOLD 또는 반대) → 단절
     * - 같은 진영이면 streak++
     *
     * 규칙:
     * - currentType 이 HOLD/알 수 없는 값 → 0
     * - 같은 날짜 row 여러 개면 가장 늦은 시각 row 채택 (manual trigger 중복 방지)
     * - operatingDates 에 없는 날(휴장/시스템 다운)은 자연스럽게 skip → 휴장 전후 streak 유지
     */
    internal fun computeStreakDays(
        histories: List<StockPickHistory>,
        currentType: String,
        operatingDates: List<LocalDate>,
    ): Int {
        val camp = when (currentType) {
            "STRONG_BUY", "BUY" -> setOf("STRONG_BUY", "BUY")
            "STRONG_SELL", "SELL" -> setOf("STRONG_SELL", "SELL")
            else -> return 0
        }

        // 같은 날짜의 가장 늦은 시각 row 만 사용. 입력 정렬 순서에 의존하지 않도록 명시적으로 maxBy.
        val typeByDate: Map<LocalDate, String> = histories
            .groupBy { it.pickDate.toLocalDate() }
            .mapValues { (_, rows) -> rows.maxBy { it.pickDate }.type }

        var streak = 0
        for (opDate in operatingDates) {
            val type = typeByDate[opDate] ?: break
            if (type !in camp) break
            streak++
        }
        return streak
    }

    fun streamRecommendations(
        req: RecommendListStreamReq
    ) {
        stockSocketClient.stockListStream(
            req = KiwoomStockStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    KiwoomStockStream(
                        item = req.items,
                        type = listOf("0B")
                    )
                )
            )
        )
    }


    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
