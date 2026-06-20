package com.example.investfeed.domain.recommend.admin.service

import com.example.investfeed.domain.recommend.admin.dto.res.AdminBackfillStatusRes
import com.example.investfeed.domain.recommend.admin.dto.res.AdminBacktestMetricsRes
import com.example.investfeed.domain.recommend.admin.dto.res.AdminMarketSnapshotRes
import com.example.investfeed.domain.recommend.admin.dto.res.AdminRecommendPickRes
import com.example.investfeed.domain.recommend.admin.dto.res.GroupMetrics
import com.example.investfeed.domain.recommend.admin.dto.res.HorizonMetrics
import com.example.investfeed.domain.recommend.entity.MarketIndexSnapshot
import com.example.investfeed.domain.recommend.entity.StockPick
import com.example.investfeed.domain.recommend.entity.StockPickHistory
import com.example.investfeed.domain.recommend.repository.MarketIndexSnapshotRepository
import com.example.investfeed.domain.recommend.repository.StockPickHistoryRepository
import com.example.investfeed.domain.recommend.repository.StockPickRepository
import com.example.investfeed.domain.papertrade.service.TrancheCalculator
import com.example.investfeed.domain.recommend.service.RecommendService
import com.example.investfeed.global.holiday.HolidayService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.sqrt

/**
 * 관리자용 추천 시스템 모니터링 + 백테스트 집계.
 *
 * **Signal Inspector (listPicks)**:
 *  - date 미지정/오늘 → stock_pick (현재 상태)
 *  - 과거 일자 → stock_pick_history (수익률 컬럼 포함)
 *
 * **Aggregate Metrics (computeMetrics)**:
 *  - 기간 내 history 의 priceOpen1d / priceClose 1d/5d/20d 로 수익률 계산
 *  - 표준 KPI: 평균 수익률, 적중률 (진영 따라), 표준편차, min/max
 *  - 분해: 매크로 시나리오 / 등급 / 진영 / 모듈 trigger 패턴
 *
 * 모든 응답은 SUPER_ADMIN / ADMIN 만 접근 (controller 의 @RequiresAction 으로 보호).
 */
@Service
class AdminRecommendMonitoringService(
    private val stockPickRepository: StockPickRepository,
    private val stockPickHistoryRepository: StockPickHistoryRepository,
    private val marketIndexSnapshotRepository: MarketIndexSnapshotRepository,
    private val indexDailyCloseRepository: com.example.investfeed.domain.index.repository.IndexDailyCloseRepository,
    private val holidayService: HolidayService,
    private val recommendService: RecommendService,
) {

    companion object {
        private val YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd")
        private const val KOSPI_CD = "001"
        private const val KOSDAQ_CD = "101"
    }

    fun listPicks(date: LocalDate?): List<AdminRecommendPickRes> {
        val today = LocalDate.now()
        val target = date ?: today

        return if (target == today) {
            val asOf = stockPickHistoryRepository.findMaxPickDate()?.toLocalDate()
            if (asOf == today) {
                stockPickRepository.findAllByOrderByStkCdAsc()
                    .map { it.toAdminRes().copy(pickDate = today) }
            } else {
                emptyList()
            }
        } else {
            stockPickHistoryRepository.findByPickDateBetween(
                target.atStartOfDay(),
                target.atTime(23, 59, 59),
            ).sortedBy { it.stkCd }.map { it.toAdminRes() }
        }
    }

    // ─── 2. Environment (매크로 스냅샷) ────────────────────────────────────

    fun listMarketSnapshots(days: Int = 30): List<AdminMarketSnapshotRes> {
        val after = LocalDate.now().minusDays(days.toLong())
        return marketIndexSnapshotRepository
            .findByCapturedDateAfterOrderByCapturedDateDesc(after)
            .map { it.toAdminRes() }
    }

    // ─── 3. Operational Health (백필 진행도) ──────────────────────────────

    fun listBackfillStatus(days: Int = 25): List<AdminBackfillStatusRes> {
        val after = LocalDate.now().minusDays(days.toLong()).atStartOfDay()
        return stockPickHistoryRepository.aggregateBackfillStatusAfter(after).map { row ->
            AdminBackfillStatusRes(
                pickDate = row[0] as LocalDate,
                totalCount = (row[1] as Number).toLong(),
                filled1d = (row[2] as Number).toLong(),
                filled5d = (row[3] as Number).toLong(),
                filled20d = (row[4] as Number).toLong(),
            )
        }
    }

    // ─── 4. Aggregate Metrics ─────────────────────────────────────────────

    /**
     * 기간 내 추천 이력의 백테스트 집계 KPI 산출.
     *
     * 수익률 정의: (priceCloseNd - priceOpen1d) / priceOpen1d × 100
     *   - 매수가: T+1일 시가 (가장 현실적인 사용자 매수 시점)
     *   - 평가가: T+N일 종가
     *
     * 적중률 정의:
     *   - BUY 진영 (originSide="BUY") : ret > 0 비율
     *   - SELL 진영 (originSide="SELL"): ret < 0 비율 (하락 = 매도 성공)
     *   - 진영 정보 없으면 ret > 0 비율 (default BUY 가정)
     */
    fun computeMetrics(periodDays: Int): AdminBacktestMetricsRes {
        val after = LocalDate.now().minusDays(periodDays.toLong()).atStartOfDay()
        val histories = stockPickHistoryRepository.findByPickDateBetween(
            after,
            LocalDate.now().atTime(23, 59, 59),
        )

        val total = histories.size

        // HOLD 는 평가 대상 X (방향성 없음). 표본 적어도 산출 진행 — 통계 신뢰도는 stdDev/표본수로 사용자가 판단.
        val evaluable = histories.filter { it.type != "HOLD" }
        if (evaluable.isEmpty()) {
            return emptyMetrics(periodDays, total, "BUY/SELL 신호 없음 (HOLD 만 존재 또는 표본 0)")
        }

        val metrics1d = computeHorizonMetrics(evaluable, horizon = "1d", n = 1) { it.priceClose1d }
        val metrics5d = computeHorizonMetrics(evaluable, horizon = "5d", n = 5) { it.priceClose5d }
        val metrics20d = computeHorizonMetrics(evaluable, horizon = "20d", n = 20) { it.priceClose20d }

        // 분해 (5d 기준 — 가장 표본 풍부할 가능성 + 단기 노이즈 + 장기 lag 사이 균형)
        val byType = decomposeBy(evaluable) { it.type }
        val byOriginSide = decomposeBy(evaluable) { it.originSide ?: "UNKNOWN" }

        return AdminBacktestMetricsRes(
            periodDays = periodDays,
            totalSignals = total,
            insufficientReason = null,
            metrics1d = metrics1d,
            metrics5d = metrics5d,
            metrics20d = metrics20d,
            byType = byType,
            byOriginSide = byOriginSide,
        )
    }

    // ─── 메트릭 계산 핵심 ────────────────────────────────────────────────

    private fun computeHorizonMetrics(
        items: List<StockPickHistory>,
        horizon: String,
        n: Int,
        closeSelector: (StockPickHistory) -> Long?,
    ): HorizonMetrics {
        val returns = items.mapNotNull { h ->
            val open = h.priceOpen1d ?: return@mapNotNull null
            val close = closeSelector(h) ?: return@mapNotNull null
            if (open <= 0L) return@mapNotNull null
            val ret = (close - open).toDouble() / open * 100.0
            h.originSide to ret
        }

        if (returns.isEmpty()) {
            return HorizonMetrics(horizon, 0, null, null, null, null, null, null)
        }

        val justReturns = returns.map { it.second }
        val mean = justReturns.average()
        val variance = justReturns.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)

        // 적중률: 진영 따라 다른 판정
        val hits = returns.count { (side, ret) ->
            when (side) {
                "BUY" -> ret > 0.0
                "SELL" -> ret < 0.0
                else -> ret > 0.0
            }
        }
        val hitRate = hits.toDouble() / returns.size * 100.0

        // 같은 표본의 같은 N영업일 시장 평균 등락률
        val marketRets = items.mapNotNull { marketRet(it, n) }
        val marketMean = if (marketRets.isEmpty()) null else marketRets.average()

        return HorizonMetrics(
            horizon = horizon,
            evaluable = returns.size,
            meanReturn = mean,
            hitRate = hitRate,
            stdDev = stdDev,
            maxReturn = justReturns.max(),
            minReturn = justReturns.min(),
            marketMeanReturn = marketMean,
        )
    }

    /**
     * 분해 집계 — 그룹별 1d/5d/20d 신호 평균 + 시장 평균 + 5d 적중률.
     */
    private fun decomposeBy(
        items: List<StockPickHistory>,
        keySelector: (StockPickHistory) -> String,
    ): List<GroupMetrics> {
        return items.groupBy(keySelector).map { (key, group) ->
            val s1 = signalMean(group) { it.priceClose1d }
            val s5 = signalMean(group) { it.priceClose5d }
            val s20 = signalMean(group) { it.priceClose20d }
            val m1 = marketMean(group, n = 1)
            val m5 = marketMean(group, n = 5)
            val m20 = marketMean(group, n = 20)
            val hit5d = hitRate(group) { it.priceClose5d }

            GroupMetrics(
                groupKey = key,
                count = group.size,
                signalMean1dPct = s1,
                marketMean1dPct = m1,
                signalMean5dPct = s5,
                marketMean5dPct = m5,
                signalMean20dPct = s20,
                marketMean20dPct = m20,
                hitRate5d = hit5d,
            )
        }.sortedByDescending { it.count }
    }

    private fun signalMean(rows: List<StockPickHistory>, close: (StockPickHistory) -> Long?): Double? {
        val rs = rows.mapNotNull { h ->
            val o = h.priceOpen1d ?: return@mapNotNull null
            val c = close(h) ?: return@mapNotNull null
            if (o <= 0L) return@mapNotNull null
            (c - o).toDouble() / o * 100.0
        }
        return if (rs.isEmpty()) null else rs.average()
    }

    private fun hitRate(rows: List<StockPickHistory>, close: (StockPickHistory) -> Long?): Double? {
        val items = rows.mapNotNull { h ->
            val o = h.priceOpen1d ?: return@mapNotNull null
            val c = close(h) ?: return@mapNotNull null
            if (o <= 0L) return@mapNotNull null
            val ret = (c - o).toDouble() / o * 100.0
            h.originSide to ret
        }
        if (items.isEmpty()) return null
        val hits = items.count { (side, ret) ->
            when (side) { "BUY" -> ret > 0.0; "SELL" -> ret < 0.0; else -> ret > 0.0 }
        }
        return hits.toDouble() / items.size * 100.0
    }

    private fun marketMean(rows: List<StockPickHistory>, n: Int): Double? {
        val rs = rows.mapNotNull { marketRet(it, n) }
        return if (rs.isEmpty()) null else rs.average()
    }

    /**
     * 한 표본의 시장 N영업일 수익률(%) — 종목 ret 정의와 동일 산식.
     * (indexClose@(pickDate+N영업일) − indexOpen@(pickDate+1영업일)) / indexOpen@(pickDate+1영업일) × 100
     * 시장구분(KOSPI/KOSDAQ)에 따라 적합한 지수 사용. 데이터 누락 시 null.
     */
    private fun marketRet(h: StockPickHistory, n: Int): Double? {
        val indsCd = when (h.marketType) {
            "KOSPI" -> KOSPI_CD
            "KOSDAQ" -> KOSDAQ_CD
            else -> return null
        }
        val pickDay = h.pickDate.toLocalDate()
        val day1 = nthNextTradingDay(pickDay, 1)
        val dayN = if (n == 1) day1 else nthNextTradingDay(pickDay, n)

        val row1 = indexDailyCloseRepository.findByIndsCdAndDt(indsCd, day1.format(YYYYMMDD)) ?: return null
        val rowN = if (n == 1) row1 else indexDailyCloseRepository.findByIndsCdAndDt(indsCd, dayN.format(YYYYMMDD)) ?: return null

        val open1 = row1.openPrice?.toDouble() ?: return null
        if (open1 <= 0.0) return null
        val closeN = rowN.closePrice.toDouble()
        return (closeN - open1) / open1 * 100.0
    }

    /**
     * pickDate 기준 N영업일 후 거래일.
     * holidayService.nextTradingDay(from) 가 이미 "from 이후의 첫 영업일" 을 반환하므로
     * 추가로 plusDays(1) 하지 않아야 한다 (이전 버그: 매 반복 +2일 점프 → 시장 평균 null).
     */
    private fun nthNextTradingDay(from: LocalDate, n: Int): LocalDate {
        var date = from
        repeat(n) {
            date = holidayService.nextTradingDay(date)
        }
        return date
    }

    private fun emptyMetrics(periodDays: Int, total: Int, reason: String) = AdminBacktestMetricsRes(
        periodDays = periodDays,
        totalSignals = total,
        insufficientReason = reason,
        metrics1d = HorizonMetrics("1d", 0, null, null, null, null, null, null),
        metrics5d = HorizonMetrics("5d", 0, null, null, null, null, null, null),
        metrics20d = HorizonMetrics("20d", 0, null, null, null, null, null, null),
        byType = emptyList(),
        byOriginSide = emptyList(),
    )

    // ─── 매핑 헬퍼 ────────────────────────────────────────────────────────

    private fun StockPickHistory.toModulePick(): StockPick = StockPick(
        type = type, stkCd = stkCd, stkNm = stkNm, marketType = marketType, originSide = originSide,
        ma5 = ma5, ma20 = ma20,
        todayChangeRate = todayChangeRate, todayVolume = todayVolume, avg20dVolume = avg20dVolume,
        rsi14 = rsi14, rsi14Breakdown70 = rsi14Breakdown70,
        high52w = high52w, low52w = low52w,
        distFromHigh52w = distFromHigh52w, distFromLow52w = distFromLow52w,
        closeAboveMa20 = closeAboveMa20, flu5Pct = flu5Pct,
    )

    private fun StockPick.toAdminRes(): AdminRecommendPickRes {
        val trig = recommendService.moduleAbsoluteTriggers(this)
        return AdminRecommendPickRes(
            stkCd = stkCd, stkNm = stkNm, marketType = marketType, originSide = originSide,
            type = type,
            effectiveType = recommendService.newEntryGrade(this),
            backboneReason = RecommendService.backboneReason(
                type, originSide, penfndK, frgnrMcapRatio, priorTrendRatio, foreignerAligned, frgnrBlocked, frgnrOppositeK,
            ),
            penfndK = penfndK, frgnrBlocked = frgnrBlocked, frgnrOppositeK = frgnrOppositeK, frgnrMcapRatio = frgnrMcapRatio,
            frgnrSameDirK = null, priorTrendRatio = priorTrendRatio, foreignerAligned = foreignerAligned,
            marketCap = null,
            pvTrigger = trig["PriceVolatility"], maTrigger = trig["MovingAverage"],
            vpTrigger = trig["VolumePrice"], rsiTrigger = trig["Rsi"],
            hl52wTrigger = trig["HighLow52w"], breakoutTrigger = trig["Breakout"],
            rsi14 = rsi14, rsi14Breakdown70 = rsi14Breakdown70,
            ma5 = ma5, ma20 = ma20, flu5Pct = flu5Pct,
            todayChangeRate = todayChangeRate, todayVolume = todayVolume, avg20dVolume = avg20dVolume,
            high52w = high52w, low52w = low52w,
            distFromHigh52w = distFromHigh52w, distFromLow52w = distFromLow52w,
            closeAboveMa20 = closeAboveMa20,
            realizedVol = realizedVol, volCapRatio = realizedVol?.let { TrancheCalculator.volCap(it) },
            pickDate = null, pickPrice = null,
            priceOpen1d = null, priceClose1d = null, priceClose5d = null, priceClose20d = null,
            ret1d = null, ret5d = null, ret20d = null,
        )
    }

    private fun StockPickHistory.toAdminRes(): AdminRecommendPickRes {
        val openOrNull = priceOpen1d?.takeIf { it > 0L }
        val ret = { close: Long? ->
            if (openOrNull != null && close != null) {
                (close - openOrNull).toDouble() / openOrNull * 100.0
            } else null
        }
        val mp = toModulePick()
        val trig = recommendService.moduleAbsoluteTriggers(mp)
        return AdminRecommendPickRes(
            stkCd = stkCd, stkNm = stkNm, marketType = marketType, originSide = originSide,
            type = type,
            effectiveType = recommendService.newEntryGrade(mp),
            backboneReason = RecommendService.backboneReason(
                type, originSide, penfndK, frgnrMcapRatio, priorTrendRatio, foreignerAligned, frgnrBlocked, frgnrOppositeK,
            ),
            penfndK = penfndK, frgnrBlocked = frgnrBlocked, frgnrOppositeK = frgnrOppositeK, frgnrMcapRatio = frgnrMcapRatio,
            frgnrSameDirK = frgnrSameDirK, priorTrendRatio = priorTrendRatio, foreignerAligned = foreignerAligned,
            marketCap = marketCap,
            pvTrigger = trig["PriceVolatility"], maTrigger = trig["MovingAverage"],
            vpTrigger = trig["VolumePrice"], rsiTrigger = trig["Rsi"],
            hl52wTrigger = trig["HighLow52w"], breakoutTrigger = trig["Breakout"],
            rsi14 = rsi14, rsi14Breakdown70 = rsi14Breakdown70,
            ma5 = ma5, ma20 = ma20, flu5Pct = flu5Pct,
            todayChangeRate = todayChangeRate, todayVolume = todayVolume, avg20dVolume = avg20dVolume,
            high52w = high52w, low52w = low52w,
            distFromHigh52w = distFromHigh52w, distFromLow52w = distFromLow52w,
            closeAboveMa20 = closeAboveMa20,
            realizedVol = realizedVol, volCapRatio = realizedVol?.let { TrancheCalculator.volCap(it) },
            pickDate = pickDate.toLocalDate(),
            pickPrice = pickPrice,
            priceOpen1d = priceOpen1d, priceClose1d = priceClose1d,
            priceClose5d = priceClose5d, priceClose20d = priceClose20d,
            ret1d = ret(priceClose1d),
            ret5d = ret(priceClose5d),
            ret20d = ret(priceClose20d),
        )
    }

    private fun MarketIndexSnapshot.toAdminRes(): AdminMarketSnapshotRes = AdminMarketSnapshotRes(
        capturedDate = capturedDate,
        kospiChangeRate = kospiChangeRate,
        kospiForeignerSign = kospiForeignerSign,
        kospiInstitutionSign = kospiInstitutionSign,
        kospiScenario = kospiScenario,
        kosdaqChangeRate = kosdaqChangeRate,
        kosdaqForeignerSign = kosdaqForeignerSign,
        kosdaqInstitutionSign = kosdaqInstitutionSign,
        kosdaqScenario = kosdaqScenario,
        capturedAt = capturedAt,
    )
}
