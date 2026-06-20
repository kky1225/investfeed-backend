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
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt
import com.example.investfeed.domain.papertrade.service.TrancheCalculator

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
    private val adjustmentModules: List<AdjustmentModule>,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String
) {
    private val log = KotlinLogging.logger {}

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
                log.warn(e) { "todayDirection 갱신 실패 stkCd=${pick.stkCd}" }
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

        if (kiwoomInvestorTradeCloseMarketRes.return_code != 0) {
            throw IllegalStateException(
                "RecommendScheduler 핵심 API(investorTradeCloseMarket) 실패 — " +
                    "return_code=${kiwoomInvestorTradeCloseMarketRes.return_code}, " +
                    "return_msg=${kiwoomInvestorTradeCloseMarketRes.return_msg}. " +
                    "stock_pick 갱신을 롤백하여 직전 거래일 데이터 보존."
            )
        }

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

        val processed: List<ProcessedPick> = buyCandidates.mapNotNull {
            val stkCd = it.stk_cd ?: return@mapNotNull null
            val stkNm = it.stk_nm ?: return@mapNotNull null
            processCandidate(stkCd, stkNm, Position.BUY, riskMap, marketTypeMap)
        } + sellCandidates.mapNotNull {
            val stkCd = it.stk_cd ?: return@mapNotNull null
            val stkNm = it.stk_nm ?: return@mapNotNull null
            processCandidate(stkCd, stkNm, Position.SELL, riskMap, marketTypeMap)
        }

        // 현재용 테이블 갱신
        stockPickRepository.deleteAll()
        stockPickRepository.saveAll(processed.map { it.toCurrentEntity() })

        // 이력용 테이블 — 같은 날 재실행 시 "오늘분"을 최신 실행분으로 교체(stk_cd·일자당 1건).
        stockPickHistoryRepository.deleteByPickDateBetween(
            now.toLocalDate().atStartOfDay(),
            now.toLocalDate().atTime(23, 59, 59),
        )
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

    private fun scenarioOf(snap: MarketMacroSnapshot?): String? {
        if (snap == null) return null
        val isUp = snap.priceChangeRate.signum() > 0
        val isDown = snap.priceChangeRate.signum() < 0
        val instBuy = snap.institutionalNetBuy > 0
        val instSell = snap.institutionalNetBuy < 0
        val frgnBuy = snap.foreignNetBuy > 0
        val frgnSell = snap.foreignNetBuy < 0
        return when {
            isUp && instBuy && frgnBuy -> "UP_BUY_BUY"           // 강세 만장일치: BUY 격상 / SELL 격하
            isUp && instBuy && frgnSell -> "UP_BUY_SELL"          // 다이버전스 — 무보정
            isUp && instSell && frgnSell -> "UP_SELL_SELL"        // 다이버전스(지수↑ + 수급↓) — 무보정
            isDown && instSell && frgnSell -> "DOWN_SELL_SELL"    // 약세 만장일치: SELL 격상 / BUY 격하
            isDown && instBuy && frgnSell -> "DOWN_BUY_SELL"      // 다이버전스 — 무보정
            isDown && instBuy && frgnBuy -> "DOWN_BUY_BUY"        // 다이버전스(지수↓ + 수급↑) — 무보정
            else -> "NEUTRAL"
        }
    }

    private fun processCandidate(
        stkCd: String,
        stkNm: String,
        position: Position,
        riskMap: Map<String, RiskFlags> = emptyMap(),
        marketTypeMap: Map<String, String> = emptyMap(),
        holdingMode: Boolean = false,
    ): ProcessedPick? {
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
            log.warn(e) { "stockInvestor 호출 실패 stkCd=$stkCd position=$position" }
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

        val window = items.take(RECENT_WINDOW + PRIOR_WINDOW)
        val penfndValues = window.map { it.penfnd_etc?.toLongOrNull() ?: 0L }
        val frgnrValues = window.map { it.frgnr_invsr?.toLongOrNull() ?: 0L }

        // 연기금 시그널 통과 못 하면 추천 풀에서 제외.
        if (!evaluateSignal(items, position)) {
            log.info {
                "[$stkNm($stkCd) $position] 시그널 미달 컷 — 연기금 시계열=$penfndValues"
            }
            return null
        }

        val penfndK = computeK(penfndValues, position)
        val frgnrBlocked = isForeignerBlocked(items, position)
        // 외국인이 추천 반대 방향으로 매매하는 강도(K). 보유평가 3티어(≥3.0/1.5~3.0/<1.5) 판정에 사용.
        val frgnrOppositeK = computeK(frgnrValues, if (position == Position.BUY) Position.SELL else Position.BUY)
        // 외국인 같은방향(동조) K — 하드스톱(연기금·외국인 둘 다 강매도) 판정 + 검증 데이터.
        val frgnrSameDirK = computeK(frgnrValues, position)
        log.info {
            "[$stkNm($stkCd) $position] 시그널 통과 — penfndK=${"%.2f".format(penfndK)}, frgnrBlocked=$frgnrBlocked, " +
                "연기금=$penfndValues, 외국인=$frgnrValues"
        }
        val pickPrice = abs(items[0].cur_prc?.toLongOrNull() ?: 0L)

        val marketCap = try {
            stockClient.stockDefaultInfo(KiwoomDefaultStockInfoReq(stk_cd = stkCd)).mac?.toLongOrNull()
        } catch (e: Exception) {
            log.warn(e) { "stockDefaultInfo 호출 실패 stkCd=$stkCd" }
            null
        }

        if (marketCap == null || marketCap == 0L) {
            log.warn {
                "시총 데이터 누락으로 추천 후보 제외 stkCd=$stkCd, stkNm=$stkNm, position=$position, marketCap=$marketCap"
            }
            return null
        }

        val frgnrSignedRatio = computeForeignerSignedMcapRatio(window, marketCap)
        val effectiveRatio = effectiveForeignerRatio(frgnrSignedRatio, position)
        val prior = penfndValues.subList(RECENT_WINDOW, penfndValues.size)
        val priorTrendRatio = computeDominantStrengthRatio(prior)
        val foreignerAligned = isForeignerDirectionallyAligned(items, position)
        val type = classify(penfndK, frgnrBlocked, frgnrOppositeK, effectiveRatio, position, prior, foreignerAligned, holdingMode)
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
            frgnrK = frgnrOppositeK,
            frgnrSameDirK = frgnrSameDirK,
            frgnrMcapRatio = frgnrSignedRatio,
            priorTrendRatio = priorTrendRatio,
            foreignerAligned = foreignerAligned,
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

            // 20일 실현변동성 (인접일 log수익률 std × √252, 연율화) — 변동성 스케일 캡 사이징용.
            val realizedVol = if (closes.size >= 21 && closes.take(21).all { it > 0L }) {
                val logRets = (0 until 20).map { ln(closes[it].toDouble() / closes[it + 1].toDouble()) }
                val mean = logRets.average()
                val variance = logRets.sumOf { (it - mean) * (it - mean) } / logRets.size
                sqrt(variance) * sqrt(252.0)
            } else null

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

            val today = closes[0]

            val defaultInfo = try {
                Thread.sleep(API_PACING_MS)
                stockClient.stockDefaultInfo(KiwoomDefaultStockInfoReq(stk_cd = stkCd))
                    .takeIf { it.return_code == 0 }
            } catch (e: Exception) {
                log.warn(e) { "ka10001 조회 실패 stkCd=$stkCd" }
                null
            }
            val high52w = defaultInfo?._250hgst?.toLongOrNull()?.let { abs(it) }
            val low52w = defaultInfo?._250lwst?.toLongOrNull()?.let { abs(it) }
            val distFromHigh52w = defaultInfo?._250hgst_pric_pre_rt?.toDoubleOrNull()
            val distFromLow52w = defaultInfo?._250lwst_pric_pre_rtm?.toDoubleOrNull()
            val closeAboveMa20 = ma20?.let { today.toDouble() > it }

            PriceMetrics(
                flu5Pct = flu5Pct,
                ma5 = ma5,
                ma20 = ma20,
                realizedVol = realizedVol,
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
            log.warn(e) { "가격 지표 계산 실패 stkCd=$stkCd" }
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
        val realizedVol: Double? = null,
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
    ): Boolean {
        val window = items.take(RECENT_WINDOW + PRIOR_WINDOW)
        if (window.size <= RECENT_WINDOW) return false
        val frgnr = window.map { it.frgnr_invsr?.toLongOrNull() ?: 0L }
        val recent = frgnr.subList(0, RECENT_WINDOW)
        val prior = frgnr.subList(RECENT_WINDOW, frgnr.size)
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
     * 1. 외국인 BLOCK → **강반대(K≥3.0) HOLD / 중간반대(1.5~3.0) 방향 유지** (추천·매매 공통 방향)
     * 2. STRONG 격상 (연기금 K ≥ 3.0 또는 외국인 시총 비중 ≥ 0.1%) + prior 추세 명확(B' ≥ 70%) → STRONG
     * 3. 외국인 시총 비중 ≥ MCAP_RATIO_BUY (0.05%) → BUY/SELL
     * 4. **(옵션 B)** 외국인 방향성 동조 (12일 추세 일관) → BUY/SELL ← 신규: 시총 비중 미달 구제
     * 5. 그 외 → HOLD
     *
     * BLOCK 처리 — 추천·매매 **방향(direction) 일치**: 외국인 강반대(K≥3.0)면 HOLD(방향 동결),
     * 중간반대(1.5~3.0)면 preBlockType(신호 방향) 유지. 매매는 여기에 더해 부분비중(10%)·하드스톱을
     * [evaluateHoldingGrade] 가 부여한다 (추천=방향 / 매매=방향+규모). holdingMode=true 는 BLOCK 시
     * preBlockType 을 그대로 둬서 evaluateHoldingGrade 가 3티어(동결/부분/전량)를 재결정하게 한다.
     *
     * @param frgnrOppositeK 외국인 추천 반대 방향 K. 강반대(≥3.0) 판정용 (매매 freeze 와 동일 지표·임계).
     * @param foreignerAligned [isForeignerDirectionallyAligned] 결과. 호출부에서 미리 계산해서 전달.
     * @param holdingMode true=BLOCK 시 preBlockType 유지(3티어는 호출부). false(추천)=강반대 HOLD/중간반대 방향 유지.
     */
    internal fun classify(
        penfndK: Double,
        frgnrBlocked: Boolean,
        frgnrOppositeK: Double,
        foreignerEffectiveRatio: Double,
        position: Position,
        prior: List<Long>,
        foreignerAligned: Boolean = false,
        holdingMode: Boolean = false,
    ): String {
        val sideName = position.name
        val priorTrendUnclear = computeDominantStrengthRatio(prior) < TREND_CLARITY_THRESHOLD

        // BLOCK 아닐 때의 분류 (BLOCK 격하 매핑에도 재사용)
        val preBlockType = when {
            penfndK >= K_STRONG_OVERRIDE && !priorTrendUnclear -> "STRONG_$sideName"
            foreignerEffectiveRatio >= MCAP_RATIO_STRONG && !priorTrendUnclear -> "STRONG_$sideName"
            foreignerEffectiveRatio >= MCAP_RATIO_BUY -> sideName
            foreignerAligned -> sideName  // 옵션 B: 시총 비중 미달이지만 외국인 12일 일관 추세
            else -> "HOLD"
        }

        if (!frgnrBlocked) return preBlockType
        if (holdingMode) return preBlockType   // 매매: 방향 유지, 3티어(동결/부분/전량)는 evaluateHoldingGrade
        // 추천도 매매와 방향 일치: 외국인 강반대(K≥3.0) → HOLD(방향 동결), 중간반대(1.5~3.0) → 방향 유지.
        return if (frgnrOppositeK >= K_FOREIGNER_STRONG) "HOLD" else preBlockType
    }

    /**
     * 연기금 K값 계산 (평균/평균 비교).
     * BUY: recent 일평균 매수 / prior 일평균 매도
     * SELL: recent 일평균 매도 / prior 일평균 매수
     * 의미: K = "매수 강도가 매도 강도의 K배" (또는 그 반대).
     */
    internal fun computeK(values: List<Long>, position: Position): Double {
        if (values.size <= RECENT_WINDOW) return 0.0
        val recent = values.subList(0, RECENT_WINDOW)
        val prior = values.subList(RECENT_WINDOW, values.size)
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
     */
    private fun computeForeignerSignedMcapRatio(
        window: List<KiwoomStockInvestor>,
        marketCap: Long,
    ): Double {
        if (marketCap <= 0L) return 0.0
        val recent = if (window.isNotEmpty()) {
            window.subList(0, minOf(RECENT_WINDOW, window.size))
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
        val frgnrK: Double,             // 외국인 추천 반대 방향 K (보유평가 3티어 판정용)
        val frgnrSameDirK: Double,      // 외국인 추천 같은방향(동조) K (하드스톱 판정/검증용)
        val frgnrMcapRatio: Double?,
        val priorTrendRatio: Double,    // B′ 연기금 prior 추세 명확성 비율 (STRONG 격상 게이트)
        val foreignerAligned: Boolean,  // 옵션B: 외국인 12일 추세 동조
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
            frgnrOppositeK = frgnrK,
            priorTrendRatio = priorTrendRatio,
            foreignerAligned = foreignerAligned,
            realizedVol = priceMetrics.realizedVol,
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
            hl52wTrigger = backtestMeta.hl52wTrigger,
            breakoutTrigger = backtestMeta.breakoutTrigger,
        )

        fun toHistoryEntity(pickDate: LocalDateTime): StockPickHistory = StockPickHistory(
            type = type,
            stkCd = stkCd,
            stkNm = stkNm,
            marketType = marketType,
            penfndK = penfndK,
            frgnrBlocked = frgnrBlocked,
            frgnrMcapRatio = frgnrMcapRatio,
            frgnrSameDirK = frgnrSameDirK,
            frgnrOppositeK = frgnrK,
            priorTrendRatio = priorTrendRatio,
            foreignerAligned = foreignerAligned,
            realizedVol = priceMetrics.realizedVol,
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
            hl52wTrigger = backtestMeta.hl52wTrigger,
            breakoutTrigger = backtestMeta.breakoutTrigger,
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
        val hl52wTrigger: String?,
        val breakoutTrigger: String?,
    ) {
        companion object {
            val EMPTY = BacktestMeta("NONE", "NONE", "NONE", "NONE", "NONE", "NONE")
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
     * 부호 일관성은 **매수(BUY) 방향에만** 요구한다(비대칭).
     * - BUY(신규 진입·보유 추가매수): 익스포저 증가 → 단발 오진입 시 실손실 → 2일 부호 일관성 유지(노이즈 차단).
     * - SELL(추천 매도리포트·보유 청산): 익스포저 감소 → 오진 시 기회비용뿐 → 일관성 면제로 빠른 청산 허용.
     *   (강도 게이트 K_SIGNAL 은 양방향 그대로 유지 → 약한 신호는 여전히 컷)
     *
     */
    internal fun evaluateSignal(
        items: List<KiwoomStockInvestor>,
        position: Position,
    ): Boolean {
        val window = items.take(RECENT_WINDOW + PRIOR_WINDOW)
        val penfnd = window.map { it.penfnd_etc?.toLongOrNull() ?: 0L }
        return evaluateColumn(
            penfnd, position, K_SIGNAL,
            requireSignConsistency = (position == Position.BUY),
        )
    }

    /**
     * 외국인이 추천 방향과 반대로 강한 시그널(K_BLOCK + 부호 일관성)이면 차단.
     *
     */
    internal fun isForeignerBlocked(
        items: List<KiwoomStockInvestor>,
        position: Position,
    ): Boolean {
        val window = items.take(RECENT_WINDOW + PRIOR_WINDOW)
        val frgnr = window.map { it.frgnr_invsr?.toLongOrNull() ?: 0L }
        val opposite = if (position == Position.BUY) Position.SELL else Position.BUY
        return evaluateColumn(frgnr, opposite, K_BLOCK, requireSignConsistency = true)
    }

    /**
     * 평균/평균 비교 식. K = "매수 강도가 매도 강도의 K배" (또는 매도 강도가 매수의 K배).
     *
     * 인덱스 구조:
     * - idx 0 ~ idx 1 = recent (당일 + 전일)
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
    ): Boolean {
        if (values.size <= RECENT_WINDOW) return false

        val recent = values.subList(0, RECENT_WINDOW)
        val prior = values.subList(RECENT_WINDOW, values.size)
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
        private const val RECENT_WINDOW = 2  // idx 0+1 = 당일+전일
        private const val PRIOR_WINDOW = 10  // idx 2~11
        private const val K_SIGNAL = 1.5          // 연기금 시그널 통과 임계 (평균/평균)
        private const val K_BLOCK = 1.5           // 외국인 차단 임계 (평균/평균)
        private const val K_STRONG_OVERRIDE = 3.0 // 연기금 STRONG 격상 임계 (평균/평균)
        private const val K_FOREIGNER_STRONG = 3.0  // 외국인 강한 반대 임계 — 보유평가 BLOCK 시 HOLD 동결
        private const val BLOCK_PARTIAL_FACTOR = 0.5 // 외국인 중간반대(1.5~3.0) 시 변동성 캡에 곱하는 배수(절반)
        private const val TREND_CLARITY_THRESHOLD = 0.7 // prior 강도 비율 임계 (미만이면 STRONG 격하)
        private const val MCAP_RATIO_BUY = 0.0005    // 0.05%
        private const val MCAP_RATIO_STRONG = 0.001  // 0.1%
        private const val MARKET_CAP_UNIT_WON = 100_000_000L  // 키움 ka10001 mac 필드: 억원 단위
        private const val API_PACING_MS = 500L
        private const val KIWOOM_BROKER_NAME = "키움증권"
        private const val STREAK_LOOKBACK_DAYS = 45L      // history / 운영 사이클 조회 기간

        // ── Stage1 절대 방향 사다리 + 점수제 ──────────────────────────────────
        // 추천·매매 공통. STRONG_SELL(0) ─ SELL(1) ─ HOLD(2) ─ BUY(3) ─ STRONG_BUY(4).
        private val GRADE_LADDER = listOf("STRONG_SELL", "SELL", "HOLD", "BUY", "STRONG_BUY")
        private const val UP_MARGIN = 2     // 매수쪽(+1) 이동 임계 (매수 신중) — 백테스트로 확정 예정
        private const val DOWN_MARGIN = 1   // 매도쪽(-1) 이동 임계 (매도 빠르게) — 백테스트로 확정 예정

        /**
         * Stage 1 — 절대 방향 한 줄 사다리 점수제 보정 (순수 함수, 테스트 대상). 추천·매매 공통.
         *
         * 사다리: STRONG_SELL(0) ~ SELL(1) ~ HOLD(2) ~ BUY(3) ~ STRONG_BUY(4).
         * 각 모듈을 **절대 기준**으로 읽어 매수쪽(bull)/매도쪽(bear) 표를 집계한다(bullVote/bearVote).
         *   net = bull - bear
         *   net >= UP_MARGIN(2)   -> 매수쪽 +1칸 (매수 신중)
         *   net <= -DOWN_MARGIN(1) -> 매도쪽 -1칸 (매도 빠르게)
         *   그 외 -> 유지
         * 이동 후 loIdx~hiIdx 로 클램프(한 사이클 1칸).
         *   - 추천: 진영 내(매수픽 HOLD~STRONG_BUY, 매도픽 STRONG_SELL~HOLD) -> 방향 못 넘음.
         *   - 매매: 전체 0~4 -> HOLD 가 반대 진영까지 넘어감(절대 투표가 방향 결정 -> originSide 불필요).
         *
         * activeModules 빈 리스트 / 시작 등급이 사다리 밖이면 원본 유지.
         * 추천의 HOLD 는 호출 전([applyAdjustments])에서 early-return.
         */
        internal fun resolveStage1Line(
            pick: StockPick,
            activeModules: List<AdjustmentModule>,
            loIdx: Int,
            hiIdx: Int,
        ): String {
            val start = GRADE_LADDER.indexOf(pick.type)
            if (start < 0 || activeModules.isEmpty()) return pick.type

            val bull = activeModules.count { bullVote(it, pick) }
            val bear = activeModules.count { bearVote(it, pick) }
            return applyTally(start, bull, bear, loIdx, hiIdx)
        }

        /**
         * 매수쪽(상향) 절대 신호 — 모듈 코드 미변경, 기존 메서드에서 추출.
         * 골든크로스/상승+거래량/저점반등/신고가/RSI<50/급등 등. (모듈은 한 픽에 한 방향만 투표 — 상호배타)
         */
        private fun bullVote(module: AdjustmentModule, pick: StockPick): Boolean =
            module.shouldPromote(pick, Position.BUY) || module.shouldDemote(pick, Position.SELL)

        /**
         * 매도쪽(하향) 절대 신호 — 모듈 코드 미변경, 기존 메서드에서 추출.
         * 데드크로스/하락+거래량/고점하락/신저가/breakdown70/급락 등.
         */
        private fun bearVote(module: AdjustmentModule, pick: StockPick): Boolean =
            module.shouldPromote(pick, Position.SELL) || module.shouldDemote(pick, Position.BUY)

        /** net(bull−bear) 마진 판정 → ±1칸 이동 → 클램프. [resolveStage1Line] 전용. */
        private fun applyTally(start: Int, bull: Int, bear: Int, loIdx: Int, hiIdx: Int): String {
            val net = bull - bear
            val moved = when {
                net >= UP_MARGIN -> start + 1
                net <= -DOWN_MARGIN -> start - 1
                else -> start
            }
            return GRADE_LADDER[moved.coerceIn(loIdx, hiIdx)]
        }

        /**
         * 백본 분류 근거 한 줄 재구성 — [classify] preBlockType 분기·상수와 동기화. 관리자 상세 표시용.
         * 저장 필드만으로 "왜 이 등급인지" 설명. priorTrendRatio(B′)/foreignerAligned 가 null(구 데이터)이면 일부 불명.
         */
        internal fun backboneReason(
            type: String,
            originSide: String?,
            penfndK: Double?,
            frgnrMcapRatio: Double?,
            priorTrendRatio: Double?,
            foreignerAligned: Boolean?,
            frgnrBlocked: Boolean?,
            frgnrOppositeK: Double?,
        ): String {
            if (originSide != Position.BUY.name && originSide != Position.SELL.name) return "-"
            val sideKr = if (originSide == Position.BUY.name) "매수" else "매도"
            val oppKStr = "%.1f".format(frgnrOppositeK ?: 0.0)
            // ① 외국인 BLOCK 2티어: 강반대(반대 K≥3.0) → HOLD 직행 / 중간반대(1.5~3.0) → 방향 유지(아래 정상 근거)
            if (frgnrBlocked == true && (frgnrOppositeK ?: 0.0) >= K_FOREIGNER_STRONG) {
                return "외국인 강반대(반대 K $oppKStr ≥ $K_FOREIGNER_STRONG) → 방향 동결 HOLD"
            }

            val k = penfndK ?: 0.0
            val kStr = "%.1f".format(k)
            // 외국인 시총비중을 추천 방향으로 정렬(양수 = 추천 방향으로 강함)
            val eff = (frgnrMcapRatio ?: 0.0).let { if (originSide == Position.BUY.name) it else -it }
            val effStr = "%.3f%%".format(eff * 100)
            val strongBar = "%.2f%%".format(MCAP_RATIO_STRONG * 100)
            val buyBar = "%.2f%%".format(MCAP_RATIO_BUY * 100)
            val bpStr = priorTrendRatio?.let { "%.2f".format(it) }
            val strongStrength = k >= K_STRONG_OVERRIDE || eff >= MCAP_RATIO_STRONG

            // 세 수급 신호를 항상 표기: ① 외국인 BLOCK ② 연기금 K ③ 외국인 시총비중
            val kMark = if (k >= K_STRONG_OVERRIDE) "≥$K_STRONG_OVERRIDE✓" else "<$K_STRONG_OVERRIDE"
            val mcapMark = when {
                eff >= MCAP_RATIO_STRONG -> "≥$strongBar✓"
                eff >= MCAP_RATIO_BUY -> "≥$buyBar(STRONG 미달)"
                else -> "미달"
            }
            val blockSig = if (frgnrBlocked == true) "외국인 중간반대(반대 K $oppKStr, 방향 유지)" else "외국인 BLOCK 없음"
            val sig = "$blockSig · 연기금 K $kStr $kMark · 외국인 시총비중 $effStr $mcapMark"

            // B′ 게이트 + 결론. B′ 값이 NULL(구 데이터)이어도 등급으로 통과/미달 추론.
            val tail = when {
                type.startsWith("STRONG") ->
                    "· B′ ${if (bpStr != null) "$bpStr ≥ $TREND_CLARITY_THRESHOLD" else "통과(값 미저장)"} → STRONG 충족 → STRONG_$sideKr"
                strongStrength ->
                    "· B′ ${if (bpStr != null) "$bpStr < $TREND_CLARITY_THRESHOLD 미달" else "미달(등급으로 추론, 값 미저장)"} → STRONG 차단 → " +
                        when {
                            eff >= MCAP_RATIO_BUY -> "시총비중 ≥$buyBar → ${sideKr}(일반)"
                            foreignerAligned == true -> "옵션B(외국인 동조) → ${sideKr}(일반)"
                            else -> "추가 근거 없음 → HOLD"
                        }
                eff >= MCAP_RATIO_BUY ->
                    "→ 시총비중 ≥$buyBar → ${sideKr}(일반)"
                foreignerAligned == true ->
                    "→ 옵션B(외국인 동조) → ${sideKr}(일반)"
                else ->
                    "→ 세 신호 모두 미달 → HOLD"
            }
            return "$sig $tail"
        }
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
        val allCodes = allItems.mapNotNull { it.stkCd?.substringBefore("_") }.distinct().joinToString("|")
        if (allCodes.isNotBlank()) {
            val kiwoomStockInterestRes = stockClient.stockInterest(req = KiwoomStockInterestReq(stk_cd = allCodes))
            if (kiwoomStockInterestRes.return_code == 0) {
                val infoMap = kiwoomStockInterestRes.atn_stk_infr
                    ?.associateBy { it.stk_cd?.substringBefore("_") } ?: emptyMap()
                allItems.forEach { item ->
                    infoMap[item.stkCd?.substringBefore("_")]?.let { info ->
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
     * 사용자 옵션 ON 인 보정 모듈들을 다수결로 평가해 effectiveType 결정.
     *
     * 룰:
     *   1. 수급 분류는 백본 — 분류 방향 (BUY ↔ SELL) 절대 못 뒤집음
     *   2. 격상은 최대 한 단계 (수익 추구는 신중)
     *   3. 격하는 다수결 1단계 / 만장일치 2단계 (손실 보호는 강하게)
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
     * 후행지표 보정: 1단계 다수결 → 2단계 매크로(동행지표) 순차 적용.
     *
     * 매크로(동행지표)는 다수결 밖에서 별도 한 단계 보정 추가 — 모듈 6가지 케이스 자체가
     * 내부 만장일치(지수 등락률 + 기관 + 외국인 합의) 메커니즘이라 단독 발동 정당.
     * 매크로 누적으로 최악 격하해도 HOLD 바닥에서 정지 (백본 횡단 없음).
     */
    private fun applyAdjustments(pick: StockPick, setting: RecommendSetting): String {
        // 추천은 진영(방향)을 못 넘는다 → 자기 진영 내로 클램프. HOLD 는 보정 대상 X(early-return).
        val (loIdx, hiIdx) = when (pick.type) {
            "STRONG_BUY", "BUY" -> 2 to 4    // [HOLD .. STRONG_BUY]
            "STRONG_SELL", "SELL" -> 0 to 2  // [STRONG_SELL .. HOLD]
            else -> return pick.type         // HOLD는 보정 대상 X
        }

        val afterScoring = resolveStage1Line(pick, activeModules(setting), loIdx, hiIdx)

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

    internal data class HoldingEvalResult(
        val stkCd: String,
        val stkNm: String,
        val type: String,
        val originSide: String?,
        val penfndK: Double?,
        val frgnrMcapRatio: Double?,
        // 결정 근거 (왜 이 등급/비중/사유인지 추적용)
        val frgnrOppositeK: Double? = null,
        val frgnrSameDirK: Double? = null,
        val priorTrendRatio: Double? = null,
        val foreignerAligned: Boolean? = null,
        val marketType: String?,
        // 평가 사유 라벨 — HARD_SELL / BLOCK_FREEZE / BLOCK_PARTIAL / CONFLICT (복수면 '|' 결합), 없으면 NULL.
        val evaluationReason: String? = null,
        // 목표 비중 — 외국인 중간반대(1.5~3.0) 부분 트림/매수 시 0.10, 그 외 NULL(기본).
        val targetWeightRatio: Double? = null,
    )

    /**
     * 매매 경로 후행지표 보정 — [applyAdjustments] 와 동일한 [resolveStage1Line] 사용.
     * 기본 클램프는 전체 [0..4](HOLD 양방향 크로싱). 외국인 BLOCK 중간반대 시 호출부가
     * 진영 내(loIdx/hiIdx)로 좁혀 STRONG/일반만 갈리게 한다. 매크로(동행지표)는 매매 경로엔 미적용.
     */
    private fun applyAdjustmentsForTrading(
        pick: StockPick,
        setting: RecommendSetting,
        loIdx: Int = 0,
        hiIdx: Int = 4,
    ): String = resolveStage1Line(pick, activeModules(setting), loIdx, hiIdx)

    /**
     * 매매 "신규진입"용 등급 — 추천과 동일한 Stage1(절대 점수제, **진영 클램프 + 전체 모듈, 매크로 제외**).
     * stock_pick.type(백본)에 모듈 보정을 입혀 추천 등급과 일치시킨다.
     * 예: BUY 백본이라도 데드크로스(매도표)면 HOLD → 신규진입 후보에서 제외(추천이 HOLD 로 거른 것과 동일).
     * resolveStage1Line 으로 모듈을 직접 재평가하므로 반대 진영 신호도 정확히 반영(저장 트리거 누락 없음).
     */
    fun newEntryGrade(pick: StockPick): String {
        val (loIdx, hiIdx) = when (pick.type) {
            "STRONG_BUY", "BUY" -> 2 to 4
            "STRONG_SELL", "SELL" -> 0 to 2
            else -> return pick.type   // HOLD 는 보정 대상 X
        }
        return resolveStage1Line(pick, adjustmentModules, loIdx, hiIdx)
    }

    fun moduleAbsoluteTriggers(pick: StockPick): Map<String, String> =
        adjustmentModules.associate { m ->
            val bull = m.shouldPromote(pick, Position.BUY) || m.shouldDemote(pick, Position.SELL)
            val bear = m.shouldPromote(pick, Position.SELL) || m.shouldDemote(pick, Position.BUY)
            m.name to when {
                bull -> "PROMOTE"
                bear -> "DEMOTE"
                else -> "NONE"
            }
        }

    /**
     * 보유 종목 1개를 추천 백본과 **동일 임계**로 재평가해 매매 등급 산출.
     *
     * 1) BUY/SELL 양 관점으로 [processCandidate] 시도(추천과 동일: K 1.5/3.0·B′·BLOCK·부호일관성).
     * 2) 결정적(non-HOLD) 결과 우선. 양방향 모두 비-HOLD 충돌 → HOLD. 둘 다 컷 → HOLD.
     * 3) HOLD 라도 보유 종목은 모듈이 움직일 수 있어야 하므로 가격/거래량 지표를 **항상 확보**
     *    (시그널 통과 픽의 priceMetrics 재사용, 둘 다 컷이면 [computePriceMetrics] 독립 호출).
     * 4) [applyAdjustmentsForTrading] 로 모듈 다수결 보정(매크로 제외, HOLD 비흡수).
     */
    internal fun evaluateHoldingGrade(stkCd: String, stkNm: String): HoldingEvalResult {
        val setting = RecommendSetting(memberId = 0L) // 첫 런: 전 모듈 ON (디폴트)
        // 보유 평가: BLOCK 시 HOLD 직행이 아니라 한 단계 격하 — 추천(opt-in) 과 달리
        // 보유 종목은 위험 관리(손절 행동 포함) 가 우선이라 STRONG 시그널 행동 보존.
        val buy = processCandidate(stkCd, stkNm, Position.BUY, holdingMode = true)
        val sell = processCandidate(stkCd, stkNm, Position.SELL, holdingMode = true)

        val conflict = buy != null && sell != null && buy.type != "HOLD" && sell.type != "HOLD"
        val chosen: ProcessedPick? = when {
            conflict -> null
            buy != null && buy.type != "HOLD" -> buy
            sell != null && sell.type != "HOLD" -> sell
            buy != null -> buy
            sell != null -> sell
            else -> null
        }

        val stockPick: StockPick
        val originSide: String?
        val penfndK: Double?
        val frgnrMcapRatio: Double?
        val marketType: String?

        val pm = buy?.priceMetrics ?: sell?.priceMetrics ?: computePriceMetrics(stkCd)
        if (chosen != null) {
            stockPick = chosen.toCurrentEntity()
            originSide = chosen.originSide
            penfndK = chosen.penfndK
            frgnrMcapRatio = chosen.frgnrMcapRatio
            marketType = chosen.marketType
        } else {
            // 양방향 컷/충돌 → HOLD. 모듈이 HOLD 를 움직일 수 있도록 지표 확보.
            marketType = buy?.marketType ?: sell?.marketType
            originSide = null
            penfndK = null
            frgnrMcapRatio = null
            stockPick = StockPick(
                type = "HOLD",
                stkCd = stkCd,
                stkNm = stkNm,
                marketType = marketType,
                flu5Pct = pm.flu5Pct,
                ma5 = pm.ma5,
                ma20 = pm.ma20,
                realizedVol = pm.realizedVol,
                avg20dVolume = pm.avg20dVolume,
                todayChangeRate = pm.todayChangeRate,
                todayVolume = pm.todayVolume,
                rsi14 = pm.rsi14,
                rsi14Breakdown70 = pm.rsi14Breakdown70,
                high52w = pm.high52w,
                low52w = pm.low52w,
                distFromHigh52w = pm.distFromHigh52w,
                distFromLow52w = pm.distFromLow52w,
                closeAboveMa20 = pm.closeAboveMa20,
            )
        }

        // 변동성 스케일 종목별 캡 — 매수 상한으로만 사용(보유 중 변동성 변화로 강제 트림 안 함).
        val volCap = TrancheCalculator.volCap(pm.realizedVol)

        // 외국인 BLOCK 3티어 (보유평가 전용) — chosen 이 방향 신호일 때만 적용.
        //   K≥3.0 → HOLD 동결(target null) / 1.5~3.0 → 진영 클램프 보정 + 목표 10% / 그 외 → 풀 클램프 + 기본 target.
        // 하드스톱 — 연기금·외국인 둘 다 강매도면 즉시 전량 청산(모듈 우회).
        //   연기금: STRONG_SELL(B′통과) + penfndK≥3.0 / 외국인: 동조 K≥3.0 + 시총비중≤−0.1%(분모폭발 보정).
        val hardStop = chosen != null
            && chosen.originSide == Position.SELL.name
            && chosen.type == "STRONG_SELL"
            && chosen.penfndK >= K_STRONG_OVERRIDE
            && chosen.frgnrSameDirK >= K_FOREIGNER_STRONG
            && (chosen.frgnrMcapRatio ?: 0.0) <= -MCAP_RATIO_STRONG
        val blocked = chosen != null && chosen.type != "HOLD" && chosen.frgnrBlocked
        val finalType: String
        val targetWeightRatio: Double?
        val tier: String?   // 사유 라벨 (어느 티어로 결정됐나)
        when {
            hardStop -> {
                finalType = "HARD_SELL"          // 즉시 전량 청산
                targetWeightRatio = null
                tier = "HARD_SELL"
            }
            blocked && chosen!!.frgnrK >= K_FOREIGNER_STRONG -> {
                finalType = "HOLD"               // 외국인 강한 반대 → 전량 보유(동결)
                targetWeightRatio = null
                tier = "BLOCK_FREEZE"
            }
            blocked -> {
                // 등급은 모듈 보정을 거치되(STRONG/일반=사이클만 갈림) 진영을 벗어나지 못하게 클램프.
                val (lo, hi) = if (chosen!!.originSide == Position.SELL.name) 0 to 1 else 3 to 4
                finalType = applyAdjustmentsForTrading(stockPick, setting, lo, hi)
                targetWeightRatio = (volCap * BLOCK_PARTIAL_FACTOR)
                    .coerceIn(TrancheCalculator.VOL_FLOOR, TrancheCalculator.W_MAX_RATIO)   // 변동성 캡 × 부분배수, floor 클램프
                tier = "BLOCK_PARTIAL"
            }
            else -> {
                finalType = applyAdjustmentsForTrading(stockPick, setting)  // 기본 풀 클램프 [0..4]
                targetWeightRatio = if (finalType == "BUY" || finalType == "STRONG_BUY") volCap else null
                tier = null
            }
        }
        // 사유 라벨 = 티어(HARD_SELL/BLOCK_FREEZE/BLOCK_PARTIAL) + CONFLICT(양방향 시그널 충돌) 결합.
        val evaluationReason = listOfNotNull(tier, if (conflict) "CONFLICT" else null)
            .joinToString("|").ifEmpty { null }
        return HoldingEvalResult(
            stkCd = stkCd, stkNm = stkNm, type = finalType,
            originSide = originSide, penfndK = penfndK, frgnrMcapRatio = frgnrMcapRatio,
            frgnrOppositeK = chosen?.frgnrK, frgnrSameDirK = chosen?.frgnrSameDirK,
            priorTrendRatio = chosen?.priorTrendRatio, foreignerAligned = chosen?.foreignerAligned,
            marketType = marketType, evaluationReason = evaluationReason,
            targetWeightRatio = targetWeightRatio,
        )
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
            hl52wTrigger = triggers["HighLow52w"] ?: "NONE",   // 누락 보강
            breakoutTrigger = triggers["Breakout"] ?: "NONE",  // 신규
        )
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
