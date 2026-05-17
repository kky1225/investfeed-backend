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
import org.springframework.stereotype.Service
import java.time.LocalDate
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
) {

    companion object {
        private const val MIN_SAMPLES_FOR_METRICS = 10
    }

    /**
     * 추천 신호 조회. date 미지정 = 오늘 = stock_pick, 과거 일자 = stock_pick_history.
     */
    fun listPicks(date: LocalDate?): List<AdminRecommendPickRes> {
        val today = LocalDate.now()
        val useCurrent = date == null || date == today

        return if (useCurrent) {
            stockPickRepository.findAllByOrderByStkCdAsc().map { it.toAdminRes() }
        } else {
            val histories = stockPickHistoryRepository.findByPickDateBetween(
                date!!.atStartOfDay(),
                date.atTime(23, 59, 59),
            )
            histories.sortedBy { it.stkCd }.map { it.toAdminRes() }
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
        if (total < MIN_SAMPLES_FOR_METRICS) {
            return emptyMetrics(periodDays, total, "데이터 부족 — 최소 $MIN_SAMPLES_FOR_METRICS 개 필요, 현재 ${total}개")
        }

        // HOLD 는 평가 대상 X (방향성 없음)
        val evaluable = histories.filter { it.type != "HOLD" }
        if (evaluable.isEmpty()) {
            return emptyMetrics(periodDays, total, "BUY/SELL 신호 없음 (HOLD 만 존재)")
        }

        val metrics1d = computeHorizonMetrics(evaluable, horizon = "1d") { it.priceClose1d }
        val metrics5d = computeHorizonMetrics(evaluable, horizon = "5d") { it.priceClose5d }
        val metrics20d = computeHorizonMetrics(evaluable, horizon = "20d") { it.priceClose20d }

        // 분해 (5d 기준 — 가장 표본 풍부할 가능성 + 단기 노이즈 + 장기 lag 사이 균형)
        val byScenario = decomposeBy(evaluable) { h ->
            // pick_date 의 매크로 스냅샷 시나리오 조회 — 매크로 환경 영향 측정 (동행지표 보정 효과 X)
            marketIndexSnapshotRepository.findByCapturedDate(h.pickDate.toLocalDate())?.kospiScenario ?: "UNKNOWN"
        }
        val byType = decomposeBy(evaluable) { it.type }
        val byOriginSide = decomposeBy(evaluable) { it.originSide ?: "UNKNOWN" }
        val byModuleTrigger = decomposeBy(evaluable) { triggerPattern(it) }

        return AdminBacktestMetricsRes(
            periodDays = periodDays,
            totalSignals = total,
            insufficientReason = null,
            metrics1d = metrics1d,
            metrics5d = metrics5d,
            metrics20d = metrics20d,
            byScenario = byScenario,
            byType = byType,
            byOriginSide = byOriginSide,
            byModuleTrigger = byModuleTrigger,
        )
    }

    // ─── 메트릭 계산 핵심 ────────────────────────────────────────────────

    private fun computeHorizonMetrics(
        items: List<StockPickHistory>,
        horizon: String,
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
            return HorizonMetrics(horizon, 0, null, null, null, null, null)
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
                else -> ret > 0.0  // default BUY 가정
            }
        }
        val hitRate = hits.toDouble() / returns.size * 100.0

        return HorizonMetrics(
            horizon = horizon,
            evaluable = returns.size,
            meanReturn = mean,
            hitRate = hitRate,
            stdDev = stdDev,
            maxReturn = justReturns.max(),
            minReturn = justReturns.min(),
        )
    }

    /**
     * 5d 기준 분해 집계. groupKey 함수로 group by, 각 그룹 내 평균 수익률/적중률.
     */
    private fun decomposeBy(
        items: List<StockPickHistory>,
        keySelector: (StockPickHistory) -> String,
    ): List<GroupMetrics> {
        return items.groupBy(keySelector).map { (key, group) ->
            val returns = group.mapNotNull { h ->
                val open = h.priceOpen1d ?: return@mapNotNull null
                val close = h.priceClose5d ?: return@mapNotNull null
                if (open <= 0L) return@mapNotNull null
                val ret = (close - open).toDouble() / open * 100.0
                h.originSide to ret
            }

            val mean = if (returns.isNotEmpty()) returns.map { it.second }.average() else null
            val hits = returns.count { (side, ret) ->
                when (side) {
                    "BUY" -> ret > 0.0
                    "SELL" -> ret < 0.0
                    else -> ret > 0.0
                }
            }
            val hitRate = if (returns.isNotEmpty()) hits.toDouble() / returns.size * 100.0 else null

            GroupMetrics(
                groupKey = key,
                count = group.size,
                evaluable5d = returns.size,
                meanReturn5d = mean,
                hitRate5d = hitRate,
            )
        }.sortedByDescending { it.count }
    }

    private fun triggerPattern(h: StockPickHistory): String {
        // 발동한 후행 trigger 만 압축 표시 (NONE/null 은 생략).
        // 매크로(동행지표) 는 의도적으로 제외 — 백테스트는 후행지표만 평가.
        val parts = listOfNotNull(
            tagged("PV", h.pvTrigger),
            tagged("MA", h.maTrigger),
            tagged("VP", h.vpTrigger),
            tagged("RSI", h.rsiTrigger),
        )
        return if (parts.isEmpty()) "ALL_NONE" else parts.joinToString(",")
    }

    private fun tagged(name: String, value: String?): String? {
        return if (value == null || value == "NONE") null else "$name=$value"
    }

    private fun emptyMetrics(periodDays: Int, total: Int, reason: String) = AdminBacktestMetricsRes(
        periodDays = periodDays,
        totalSignals = total,
        insufficientReason = reason,
        metrics1d = HorizonMetrics("1d", 0, null, null, null, null, null),
        metrics5d = HorizonMetrics("5d", 0, null, null, null, null, null),
        metrics20d = HorizonMetrics("20d", 0, null, null, null, null, null),
        byScenario = emptyList(),
        byType = emptyList(),
        byOriginSide = emptyList(),
        byModuleTrigger = emptyList(),
    )

    // ─── 매핑 헬퍼 ────────────────────────────────────────────────────────

    /** stock_pick (현재 상태) — 가격/수익률 컬럼은 null. */
    private fun StockPick.toAdminRes(): AdminRecommendPickRes = AdminRecommendPickRes(
        stkCd = stkCd, stkNm = stkNm, marketType = marketType, originSide = originSide,
        type = type,
        pvTrigger = pvTrigger, maTrigger = maTrigger, vpTrigger = vpTrigger, rsiTrigger = rsiTrigger,
        rsi14 = rsi14, rsi14Breakdown70 = rsi14Breakdown70,
        ma5 = ma5, ma20 = ma20, flu5Pct = flu5Pct,
        todayChangeRate = todayChangeRate, todayVolume = todayVolume, avg20dVolume = avg20dVolume,
        high52w = high52w, low52w = low52w,
        distFromHigh52w = distFromHigh52w, distFromLow52w = distFromLow52w,
        closeAboveMa20 = closeAboveMa20,
        pickDate = null, pickPrice = null,
        priceOpen1d = null, priceClose1d = null, priceClose5d = null, priceClose20d = null,
        ret1d = null, ret5d = null, ret20d = null,
    )

    /** stock_pick_history — 가격 + 수익률 계산 포함. */
    private fun StockPickHistory.toAdminRes(): AdminRecommendPickRes {
        val openOrNull = priceOpen1d?.takeIf { it > 0L }
        val ret = { close: Long? ->
            if (openOrNull != null && close != null) {
                (close - openOrNull).toDouble() / openOrNull * 100.0
            } else null
        }
        return AdminRecommendPickRes(
            stkCd = stkCd, stkNm = stkNm, marketType = marketType, originSide = originSide,
            type = type,
            pvTrigger = pvTrigger, maTrigger = maTrigger, vpTrigger = vpTrigger, rsiTrigger = rsiTrigger,
            rsi14 = rsi14, rsi14Breakdown70 = rsi14Breakdown70,
            ma5 = ma5, ma20 = ma20, flu5Pct = flu5Pct,
            todayChangeRate = todayChangeRate, todayVolume = todayVolume, avg20dVolume = avg20dVolume,
            high52w = high52w, low52w = low52w,
            distFromHigh52w = distFromHigh52w, distFromLow52w = distFromLow52w,
            closeAboveMa20 = closeAboveMa20,
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
