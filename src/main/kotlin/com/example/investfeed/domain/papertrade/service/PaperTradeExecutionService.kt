package com.example.investfeed.domain.papertrade.service

import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.papertrade.entity.PaperFill
import com.example.investfeed.domain.papertrade.repository.HoldingGradeRepository
import com.example.investfeed.domain.papertrade.repository.PaperFillRepository
import com.example.investfeed.domain.recommend.entity.RiskPreset
import com.example.investfeed.domain.recommend.repository.StockPickRepository
import com.example.investfeed.domain.recommend.service.RecommendService
import com.example.investfeed.domain.stock.dto.req.StockInfoListReq
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomDepositReq
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomHoldingReq
import com.example.investfeed.kiwoom.order.client.KiwoomOrderClient
import com.example.investfeed.kiwoom.order.client.MockAccountClient
import com.example.investfeed.kiwoom.order.dto.req.KiwoomCancelOrderReq
import com.example.investfeed.kiwoom.order.dto.req.KiwoomOrderReq
import com.example.investfeed.kiwoom.order.dto.req.KiwoomPendingOrderReq
import com.example.investfeed.kiwoom.stock.client.StockClient
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PaperTradeExecutionService(
    private val kiwoomOrderClient: KiwoomOrderClient,
    private val mockAccountClient: MockAccountClient,
    private val stockClient: StockClient,
    private val trancheCalculator: TrancheCalculator,
    private val paperFillRepository: PaperFillRepository,
    private val holdingGradeRepository: HoldingGradeRepository,
    private val stockPickRepository: StockPickRepository,
    private val recommendService: RecommendService,
    private val holidayService: HolidayService,
    private val schedulerLogService: SchedulerLogService,
    private val authClient: AuthClient,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val ORDER_PACING_MS = 1500L
        private const val MAX_CONCURRENT_HOLDINGS = 20
        private val CYCLE_SIDES = listOf("BUY", "SELL")
        private const val SECOND_PHASE_ATTEMPTS = 5       // 2차 매수 시도 횟수 (09:01~03, 30초 간격)
        private const val SECOND_PHASE_RETRY_MS = 30_000L // 시가 단일가 임의연장(~30초) + 반영 지연 감안
        private const val PHASE_FIRST = 1                 // paper_fill.phase — 08:50 1차
        private const val PHASE_SECOND = 2                // paper_fill.phase — 09:01 2차 (잔여 차감 기준)
    }

    private data class HeldPos(val qty: Long, val price: Long)
    private data class LastCycle(val side: String, val index: Int)

    private data class ExecContext(
        val nav: Long,                        // 총 평가액 (현금 + 보유평가)
        val availableCash: Long,              // 주문가능금액
        val holdings: Map<String, HeldPos>,   // 정규화 stkCd → 보유
        val grades: Map<String, String>,      // 정규화 stkCd → 등급
        val targetRatios: Map<String, Double?>, // 정규화 stkCd → 목표 비중(보유=holding_grade volCap×block / 신규진입=volCap)
        val seedPrices: Map<String, Long>,    // 정규화 stkCd → 신규진입 사이징가(ma5)
        val riskBlocked: Set<String>,         // ⑤-a NORMAL 차단(정리매매·투자위험) — 신규진입만 적용
        val lastCycles: Map<String, LastCycle>, // 정규화 stkCd → 직전 사이클 체결(보유 종목만 조회)
        val explicitHolds: Map<String, String?>, // holding_grade 명시 type=HOLD 보유 (코드 → origin_side, 회수 1~3티어용)
        val moduleHalfCodes: Set<String>,        // MODULE_HALF 라벨 보유 (모듈 승급 BUY — 회수 4티어: 목표 초과분 회수용)
        val pickK: Map<String, Double>,          // 정규화 stkCd → 당일 픽 연기금 K (신규진입 동급 내 우선순위용)
        val gradeTags: Map<String, String>,      // 정규화 stkCd → holding_grade.evaluation_reason (거래 기록 note 에 등급 출처 표기)
    ) {
        /** note 용 등급 라벨 — "BUY·ACCUM" / "BUY·MODULE_HALF" 처럼 등급 출처를 붙여 거래내역에서 경로를 구분한다. */
        fun gradeLabel(stkCd: String, grade: String): String =
            gradeTags[stkCd]?.let { "$grade·$it" } ?: grade
    }

    private data class OrderCandidate(
        val stkCd: String,
        val side: TrancheSide,
        val qty: Long,
        val grade: String,
        val price: Long,   // 사이징/현금배분 환산가(시장가라 실제 체결가는 시가)
        val cycleIndex: Int, // 이번 주문의 사이클 회차(1부터)
        val recoveryOrigin: String? = null, // ⑥-b 현금회수 매도만 non-null (수급 티어 라벨 — note 구분용)
    )

    private data class SkipCandidate(
        val stkCd: String,
        val qty: Long,       // 미발행 잔여 수량 (부분 매수 시 잔여분만)
        val price: Long,
        val grade: String,
        val newEntry: Boolean,
        val cycleIndex: Int,
        val slotWait: Boolean = false, // true=만석이지만 당일 전량 매도 발행분만큼 슬롯이 열릴 예정 — 2차(09:01)에서 보유 수 재확인 후 진입
    )


    fun runPaperTradeExec() {
        schedulerLogService.execute(SchedulerName.PaperTradeExecScheduler) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
                doPaperTradeExec()
            } finally {
                SecurityContextHolder.clearContext()
            }
        }
    }

    private fun doPaperTradeExec() {
        // ① 전날 미체결 취소 (오늘 새 주문 전. 등급은 매일 재평가되므로 잔여 주문은 stale)
        cancelPreviousUnfilled()

        // ②③ 계좌 재조회 + 등급 로드 → 트랜치/주문의 입력 컨텍스트
        val ctx = buildExecContext()
        log.info {
            "PaperTradeExecScheduler ctx — NAV=${ctx.nav}, 가용현금=${ctx.availableCash}, " +
                "보유=${ctx.holdings.size}종목, 등급=${ctx.grades.size}종목"
        }

        // ④ 종목별 희망 주문 산출 (+ 만석 시 전량 매도 예정분만큼의 슬롯대기 SKIP)
        val (candidates, slotWaits) = buildOrderCandidates(ctx)
        log.info {
            "PaperTradeExecScheduler 주문후보 ${candidates.size}건" +
                (if (slotWaits.isNotEmpty()) ", 슬롯대기 ${slotWaits.size}건" else "") + ": " +
                candidates.joinToString { "${it.stkCd}:${it.side}:${it.qty}(${it.grade})" }
        }

        // ⑤-b 거래정지 제외 (보유 매도 포함 전 후보). ⑤-a(성향필터)는 buildOrderCandidates에서 적용됨.
        val halted = fetchHaltedCodes()
        val filtered = candidates.filter { it.stkCd !in halted }
        if (filtered.size != candidates.size) {
            log.info { "PaperTradeExecScheduler: 거래정지 ${candidates.size - filtered.size}건 제외" }
        }

        // ⑥ 현금 STRONG우선 배분: 매도는 전부 발행, 매수는 STRONG_BUY 우선 + 부분 매수(살 수 있는 만큼),
        // 잔여 수량은 SKIP — 기록(측정) 및 2차 매수(09:01) 입력.
        val sells = filtered.filter { it.side == TrancheSide.SELL }
        val buys = filtered.filter { it.side == TrancheSide.BUY }
            .sortedBy { if (it.grade == "STRONG_BUY") 0 else 1 }
        val fundedBuys = mutableListOf<OrderCandidate>()
        val skips = mutableListOf<SkipCandidate>()
        var cashLeft = ctx.availableCash
        for (b in buys) {
            val affordable = minOf(b.qty, cashLeft / b.price)
            if (affordable >= 1L) {
                fundedBuys += if (affordable == b.qty) b else b.copy(qty = affordable)
                cashLeft -= affordable * b.price
            }
            val rest = b.qty - affordable
            if (rest >= 1L) {
                skips += SkipCandidate(b.stkCd, rest, b.price, b.grade, b.stkCd !in ctx.holdings, b.cycleIndex)
                log.info { "PaperTradeExecScheduler: 현금부족 스킵 ${b.stkCd}(${b.grade}) ${rest}주 left=$cashLeft" }
            }
        }

        // 슬롯대기 SKIP 합류 (거래정지 제외) — 부족액 산정·⑧-b 기록·2차 매수 입력을 현금 스킵과 동일 경로로 공유
        skips += slotWaits.filter { it.stkCd !in halted }
        // 2차 발행 순서는 SKIP 행 id 순 — 등급 → 동급 내 연기금 K 큰 순으로 정렬해 기록 순서 = 우선순위가 되게 한다
        skips.sortWith(compareBy({ if (it.grade == "STRONG_BUY") 0 else 1 }, { -(ctx.pickK[it.stkCd] ?: 0.0) }))

        // ⑥-b 현금회수 매도 — 신규진입 스킵 부족액만큼 HOLD 보유를 트랜치 매도해 2차 매수 자금 확보
        val recoverySells = buildRecoverySells(ctx, sells, skips, halted)
        val toOrder = sells + recoverySells + fundedBuys

        // ⑦ 시장가 주문(동시호가 창 제출) + ⑧ paper_fill 행동 로그. 거부는 격리(스킵+기록, 진행).
        val today = LocalDate.now()
        var ok = 0
        for ((i, c) in toOrder.withIndex()) {
            if (i > 0) Thread.sleep(ORDER_PACING_MS)
            try {
                val req = KiwoomOrderReq(dmst_stex_tp = "KRX", stk_cd = c.stkCd, ord_qty = c.qty.toString())
                val res = if (c.side == TrancheSide.BUY)
                    kiwoomOrderClient.placeBuyOrder(req) else kiwoomOrderClient.placeSellOrder(req)
                paperFillRepository.save(
                    PaperFill(
                        stkCd = c.stkCd, side = c.side.name, fillDate = today,
                        quantity = c.qty, price = c.price, kiwoomOrderNo = res.ord_no,
                        cycleIndex = c.cycleIndex, grade = c.grade,
                        newEntry = if (c.side == TrancheSide.BUY) c.stkCd !in ctx.holdings else null,
                        phase = PHASE_FIRST,
                        note = c.recoveryOrigin
                            ?.let { "현금회수 매도($it, 사이징가=${c.price}, ${c.cycleIndex}회차)" }
                            ?: "시장가 동시호가 제출(등급=${ctx.gradeLabel(c.stkCd, c.grade)}, 사이징가=${c.price}, ${c.cycleIndex}회차)",
                    )
                )
                ok++
            } catch (e: Exception) {
                log.error(e) { "주문 실패 ${c.stkCd} ${c.side} ${c.qty}" }
                paperFillRepository.save(
                    PaperFill(
                        stkCd = c.stkCd, side = "REJ", fillDate = today,
                        quantity = c.qty, price = c.price, kiwoomOrderNo = null,
                        grade = c.grade, phase = PHASE_FIRST,
                        note = "주문거부/실패(${c.side.name}): ${e.message?.take(170)}",
                    )
                )
            }
        }
        // ⑧-b 스킵 기록 — 1차 시점 부족분 측정 + 2차 매수(09:01)의 입력 (DB 가 인계 매체, 재시작 안전)
        skips.forEach { s ->
            paperFillRepository.save(
                PaperFill(
                    stkCd = s.stkCd, side = "SKIP", fillDate = today,
                    quantity = s.qty, price = s.price, cycleIndex = s.cycleIndex,
                    grade = s.grade, newEntry = s.newEntry, phase = PHASE_FIRST,
                    note = if (s.slotWait) "슬롯대기 스킵(등급=${s.grade}, 전량매도 발행분 슬롯 개방 대기)"
                    else "현금부족 스킵(등급=${ctx.gradeLabel(s.stkCd, s.grade)}, ${if (s.newEntry) "신규진입" else "보유추가"})",
                )
            )
        }

        log.info {
            "PaperTradeExecScheduler 완료 — 후보 ${candidates.size}, 발행대상 ${toOrder.size}" +
                "(매도 ${sells.size}/회수 ${recoverySells.size}/매수 ${fundedBuys.size}), " +
                "스킵 ${skips.size}, 성공 $ok"
        }
    }

    private fun buildExecContext(): ExecContext {
        val deposit = mockAccountClient.deposit(KiwoomDepositReq(qry_tp = "3"))
        val availableCash = parseAmt(deposit.ord_alow_amt) .takeIf { it > 0 } ?: parseAmt(deposit.entr)

        val holdingRes = mockAccountClient.holdingList(KiwoomHoldingReq(qry_tp = "1", dmst_stex_tp = "KRX"))
        val rows = holdingRes?.acnt_evlt_remn_indv_tot.orEmpty()
        val holdings = rows.mapNotNull { h ->
            val cd = normCd(h.stk_cd) ?: return@mapNotNull null
            val qty = parseAmt(h.rmnd_qty)
            if (qty <= 0L) return@mapNotNull null
            cd to HeldPos(qty = qty, price = parseAmt(h.cur_prc))
        }.toMap()
        val totEvlt = parseAmt(holdingRes?.tot_evlt_amt)
        val nav = parseAmt(holdingRes?.prsm_dpst_aset_amt).takeIf { it > 0 } ?: (availableCash + totEvlt)

        // ③ 등급: 직전 거래일 holding_grade + 당일 stock_pick(추천) 병합.
        val priorTradingDay = holidayService.lastTradingDay(LocalDate.now().minusDays(1))
        val grades = mutableMapOf<String, String>()
        val targetRatios = mutableMapOf<String, Double?>()
        val seedPrices = mutableMapOf<String, Long>()
        val riskBlocked = mutableSetOf<String>()
        val pickK = mutableMapOf<String, Double>()
        val normalBlocked = RiskPreset.NORMAL.blockedCategories() // {정리매매, 투자위험}
        stockPickRepository.findAll().forEach { p ->
            val cd = normCd(p.stkCd) ?: return@forEach
            p.penfndK?.let { pickK[cd] = it }
            // 신규진입 등급 = 추천과 동일한 Stage1(절대 점수제, 진영 클램프, 전체 모듈, 매크로 제외).
            // 백본(p.type) 그대로 쓰면 데드크로스 등 모듈 격하를 못 봐서 추천(HOLD)과 어긋남 → 추천 등급으로 통일.
            grades[cd] = recommendService.newEntryGrade(p)
            // 신규진입 캡 = 변동성 스케일 volCap(저장된 realized_vol). 보유분이면 아래 holding_grade 로 덮어씀.
            targetRatios[cd] = TrancheCalculator.volCap(p.realizedVol)
            // 신규 진입 사이징가 = ma5(저장값, 라이브 호출 X). 어차피 시장가 시가 체결이라 근사 충분.
            p.ma5?.takeIf { it > 0 }?.let { seedPrices[cd] = it.toLong() }
            // ⑤-a 성향필터: NORMAL=정리매매/투자위험 종목은 신규 진입 차단(보유는 면제 — 별도 적용)
            if (normalBlocked.any { it.matches(p) }) riskBlocked += cd
        }
        val explicitHolds = mutableMapOf<String, String?>()
        val moduleHalfCodes = mutableSetOf<String>()
        val gradeTags = mutableMapOf<String, String>()
        holdingGradeRepository.findByEvalDate(priorTradingDay).forEach { g ->
            val cd = normCd(g.stkCd) ?: return@forEach
            if (cd in holdings) {
                grades[cd] = g.type
                targetRatios[cd] = g.targetWeightRatio
                // 명시적 HOLD 만 회수 매도 대상 (평가 누락으로 기본 HOLD 처리된 종목은 제외).
                if (g.type == "HOLD") explicitHolds[cd] = g.originSide
                // 모듈 승급 BUY(반 비중) — 등급 계층이 붙인 라벨을 소비 (회수 4티어 대상 식별).
                if (g.evaluationReason?.contains("MODULE_HALF") == true) moduleHalfCodes += cd
                g.evaluationReason?.takeIf { it.isNotBlank() }?.let { gradeTags[cd] = it }
            }
        }
        val lastCycles = holdings.keys.mapNotNull { cd ->
            paperFillRepository
                .findFirstByStkCdAndSideInOrderByFillDateDescIdDesc(cd, CYCLE_SIDES)
                ?.let { cd to LastCycle(it.side, it.cycleIndex ?: 1) }
        }.toMap()

        return ExecContext(
            nav = nav, availableCash = availableCash,
            holdings = holdings, grades = grades, targetRatios = targetRatios,
            seedPrices = seedPrices, riskBlocked = riskBlocked, lastCycles = lastCycles,
            explicitHolds = explicitHolds, moduleHalfCodes = moduleHalfCodes, pickK = pickK,
            gradeTags = gradeTags,
        )
    }

    /**
     * ⑥-b 현금회수 매도 — 매수 스킵 부족액만큼 HOLD 보유를 부분 매도해 2차 매수(09:01) 자금 확보.
     *
     * 순서는 수급 4티어:
     *   1~3. HOLD (origin_side): SELL 기원 → null(중립) → BUY 기원. 티어 내 보유금액 큰 순.
     *   4.   MODULE_HALF BUY 의 **목표(반 비중) 초과분** — 등급은 BUY 유지, floor=반 비중 목표까지만
     *        회수(초과 종목은 room≤0 이라 매수 후보가 없어 자기모순 불가). 티어 내 초과금액 큰 순.
     * 종목당 min(SELL 트랜치, 남은부족액) — 전량 강제 없음, 부족액 못 채우면 못 채운 채 종료.
     */
    private fun buildRecoverySells(
        ctx: ExecContext,
        sells: List<OrderCandidate>,
        skips: List<SkipCandidate>,
        halted: Set<String>,
    ): List<OrderCandidate> {
        val gradeSellProceeds = sells.sumOf { it.qty * it.price }
        var remaining = skips.sumOf { it.qty * it.price } - gradeSellProceeds
        if (remaining <= 0L) return emptyList()

        val result = mutableListOf<OrderCandidate>()
        val holds = ctx.holdings
            .filterKeys { it in ctx.explicitHolds && it !in halted }
            .entries
            .sortedWith(
                compareBy<Map.Entry<String, HeldPos>> {
                    when (ctx.explicitHolds[it.key]) { "SELL" -> 0; null -> 1; else -> 2 }
                }.thenByDescending { it.value.qty * it.value.price }
            )
        for ((cd, pos) in holds) {
            if (remaining <= 0L) break
            if (pos.price <= 0L) continue
            val o = trancheCalculator.calculate("SELL", pos.qty, pos.price, ctx.nav, null, soldCycles(ctx, cd))
            if (o.side != TrancheSide.SELL || o.qty <= 0L) continue
            val capped = minOf(o.qty, maxOf(1L, remaining / pos.price))
            val originLabel = when (ctx.explicitHolds[cd]) { "SELL" -> "HOLD·SELL기원"; "BUY" -> "HOLD·BUY기원"; else -> "HOLD·중립" }
            result += OrderCandidate(
                cd, TrancheSide.SELL, capped, "HOLD", pos.price,
                nextCycleIndex(ctx, cd, TrancheSide.SELL), recoveryOrigin = originLabel,
            )
            remaining -= capped * pos.price
        }

        // 4티어 — HOLD 로 부족하면 모듈 승급 BUY(MODULE_HALF)의 목표 초과분 회수.
        // 등급은 BUY 유지: 평소엔 동결(매수 정지)일 뿐이지만, 현금이 필요할 때는 등급 계층이
        // "여기 있으면 안 된다"고 판정한 초과 자본부터 순수 수급 신호로 옮긴다. floor=반 비중 목표.
        if (remaining > 0L) {
            val halfOvers = ctx.holdings
                .filterKeys { it in ctx.moduleHalfCodes && it !in halted }
                .entries
                .mapNotNull { (cd, pos) ->
                    val floor = ctx.targetRatios[cd] ?: return@mapNotNull null
                    if (pos.price <= 0L) return@mapNotNull null
                    val excess = pos.qty * pos.price - (floor * ctx.nav).toLong()
                    if (excess > 0L) Triple(cd, pos, floor) to excess else null
                }
                .sortedByDescending { it.second } // 초과금액 큰 순 — 최소 종목 수로 부족액 커버
            for ((entry, _) in halfOvers) {
                if (remaining <= 0L) break
                val (cd, pos, floor) = entry
                val o = trancheCalculator.calculate("SELL", pos.qty, pos.price, ctx.nav, floor, soldCycles(ctx, cd))
                if (o.side != TrancheSide.SELL || o.qty <= 0L) continue
                val capped = minOf(o.qty, maxOf(1L, remaining / pos.price))
                result += OrderCandidate(
                    cd, TrancheSide.SELL, capped, "BUY", pos.price,
                    nextCycleIndex(ctx, cd, TrancheSide.SELL), recoveryOrigin = "모듈BUY초과",
                )
                remaining -= capped * pos.price
            }
        }

        if (result.isNotEmpty()) {
            log.info {
                "PaperTradeExecScheduler 현금회수 매도 ${result.size}건 (등급매도대금 $gradeSellProceeds 선차감): " +
                    result.joinToString { "${it.stkCd}:${it.qty}주(${it.recoveryOrigin})" }
            }
        }
        return result
    }

    fun runSecondPhaseBuys() {
        val today = LocalDate.now()
        schedulerLogService.execute(SchedulerName.PaperTradeSecondBuyScheduler) {
            if (paperFillRepository.findByFillDateAndSide(today, "SKIP").isEmpty()) {
                log.info { "PaperTradeSecondBuyScheduler: 당일 스킵 없음 — 종료" }
                return@execute
            }
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
                doSecondPhaseBuys(today)
            } finally {
                SecurityContextHolder.clearContext()
            }
        }
    }

    /** 당일 SKIP 대비 미발행 잔여 후보 산출 — 같은 종목 SKIP 이 중복이면 최신 행(수동 재실행 대비). */
    private fun loadRemainingSkips(today: LocalDate): List<SkipCandidate> {
        val issued = (paperFillRepository.findByFillDateAndSide(today, "BUY") +
            paperFillRepository.findByFillDateAndSide(today, "REJ"))
            .filter { it.phase == PHASE_SECOND }
            .groupBy { it.stkCd }
            .mapValues { (_, rows) -> rows.sumOf { it.quantity } }

        return paperFillRepository.findByFillDateAndSide(today, "SKIP")
            .groupBy { it.stkCd }
            .mapNotNull { (_, rows) -> rows.maxByOrNull { it.id } }
            // 발행 우선순위: STRONG_BUY 신규 → BUY 신규 → 보유 추가매수.
            // 동급 내에서는 id 순 — 1차(⑧-b)가 등급 → 연기금 K 큰 순으로 정렬해 저장하므로 id 순 = K 순.
            .sortedWith(compareBy({ it.newEntry != true }, { if (it.grade == "STRONG_BUY") 0 else 1 }, { it.id }))
            .mapNotNull { s ->
                val remaining = s.quantity - (issued[s.stkCd] ?: 0L)
                if (remaining < 1L || s.price <= 0L) null
                else SkipCandidate(
                    stkCd = s.stkCd, qty = remaining, price = s.price,
                    grade = s.grade ?: "BUY", newEntry = s.newEntry ?: false,
                    cycleIndex = s.cycleIndex ?: 1,
                )
            }
    }

    private fun doSecondPhaseBuys(today: LocalDate) {
        var items = loadRemainingSkips(today)
        if (items.isEmpty()) {
            log.info { "PaperTradeSecondBuyScheduler: 스킵 전량 기발행 — 종료(멱등)" }
            return
        }
        for (attempt in 1..SECOND_PHASE_ATTEMPTS) {
            if (attempt > 1) Thread.sleep(SECOND_PHASE_RETRY_MS)
            val dep = mockAccountClient.deposit(KiwoomDepositReq(qry_tp = "3"))
            var cash = parseAmt(dep.ord_alow_amt)
            // 보유 수 재조회 — 슬롯대기 신규진입은 1차 전량 매도가 실제 체결되어 슬롯이 열렸을 때만 발행
            // (매도 미체결이면 만석 그대로 → 발행 안 함 → 캡 초과 없음. 조회 실패 시 보수적으로 슬롯 0)
            val heldNow = mockAccountClient.holdingList(KiwoomHoldingReq(qry_tp = "1", dmst_stex_tp = "KRX"))
                ?.acnt_evlt_remn_indv_tot.orEmpty()
                .filter { parseAmt(it.rmnd_qty) > 0L }
                .mapNotNull { normCd(it.stk_cd) }
                .toSet()
            var slots = (MAX_CONCURRENT_HOLDINGS - heldNow.size).coerceAtLeast(0)
            // 회차별 주문가능금액 = 시가 매도 대금 반영 추적 — 모의서버 재사용금 지원 여부의 실측 근거
            log.info { "PaperTradeSecondBuyScheduler ${attempt}회차 — 주문가능금액=$cash, 보유=${heldNow.size}종목(슬롯 $slots), 잔여후보 ${items.size}건" }
            val unfilled = mutableListOf<SkipCandidate>()
            for (s in items) {
                val needsSlot = s.newEntry && s.stkCd !in heldNow
                if (needsSlot && slots < 1) {
                    unfilled += s   // 슬롯 미개방(매도 미체결 등) — 다음 회차 재확인
                    continue
                }
                val affordable = if (s.price > 0L) minOf(s.qty, cash / s.price) else 0L
                if (affordable < 1L) {
                    unfilled += s
                    continue
                }
                Thread.sleep(ORDER_PACING_MS)
                try {
                    val req = KiwoomOrderReq(dmst_stex_tp = "KRX", stk_cd = s.stkCd, ord_qty = affordable.toString())
                    val res = kiwoomOrderClient.placeBuyOrder(req)
                    paperFillRepository.save(
                        PaperFill(
                            stkCd = s.stkCd, side = "BUY", fillDate = today,
                            quantity = affordable, price = s.price, kiwoomOrderNo = res.ord_no,
                            cycleIndex = s.cycleIndex, grade = s.grade,
                            newEntry = s.newEntry, phase = PHASE_SECOND,
                            note = "시장가 2차 발행(등급=${s.grade}, 사이징가=${s.price}, 시도 ${attempt}회차)",
                        )
                    )
                    cash -= affordable * s.price
                    if (needsSlot) slots--
                    val rest = s.qty - affordable
                    if (rest >= 1L) unfilled += s.copy(qty = rest)
                } catch (e: Exception) {
                    log.error(e) { "2차 매수 실패 ${s.stkCd} ${affordable}주" }
                    paperFillRepository.save(
                        PaperFill(
                            // REJ 도 phase=2 로 기록 → 잔여 차감에 포함되어 재시작 후에도 재시도하지 않음(기존 정책 유지)
                            stkCd = s.stkCd, side = "REJ", fillDate = today,
                            quantity = affordable, price = s.price,
                            grade = s.grade, phase = PHASE_SECOND,
                            note = "주문거부/실패(2차 BUY): ${e.message?.take(170)}",
                        )
                    )
                }
            }
            items = unfilled
            if (items.isEmpty()) break
        }
        log.info {
            if (items.isEmpty()) "PaperTradeSecondBuyScheduler 완료 — 스킵 전량 소화"
            else "PaperTradeSecondBuyScheduler 완료 — 미소화 ${items.size}건 소멸(익일 정상 사이클)"
        }
    }

    private fun nextCycleIndex(ctx: ExecContext, stkCd: String, side: TrancheSide): Int {
        val last = ctx.lastCycles[stkCd] ?: return 1
        return if (last.side == side.name) last.index + 1 else 1
    }

    private fun soldCycles(ctx: ExecContext, stkCd: String): Int {
        val last = ctx.lastCycles[stkCd] ?: return 0
        return if (last.side == TrancheSide.SELL.name) last.index else 0
    }

    /**
     * ④ 종목별 희망 주문 산출 (TrancheCalculator).
     * - 보유 종목: grades 등급(없으면 HOLD=동결)으로 평가, 가격=조회 현재가.
     * - 비보유 종목: stock_pick STRONG_BUY/BUY 만 신규 진입 후보(HOLD/SELL 등은 무행동 — 없는 걸 못 팖).
     *   가격=ma5(없으면 스킵). 동시 보유 ≤20 캡(신규 진입 한정), 우선순위 = 등급 → 동급 내 연기금 K 큰 순.
     * - 만석 시 이번 사이클 전량 매도 발행 수만큼 슬롯대기 SKIP 반환(second = 2차 09:01 인계분).
     * 현금 STRONG우선 배분·거래정지/상하한 사전필터·실제 발행은 서브스텝 4.
     *
     * 동시 보유 캡은 모의투자 수익률 검증 단계에서 표본 확보를 위해 10→20 으로 확장(2026-05-28).
     * 실투자 단계에서는 현금 비중 정책과 함께 재검토 예정.
     */
    private fun buildOrderCandidates(ctx: ExecContext): Pair<List<OrderCandidate>, List<SkipCandidate>> {
        val candidates = mutableListOf<OrderCandidate>()

        // 보유 종목 — 등급대로(HOLD/SELL/STRONG_SELL 등 전부). 목표비중(holding_grade volCap×block)도 전달.
        for ((cd, pos) in ctx.holdings) {
            val grade = ctx.grades[cd] ?: "HOLD"
            val o = trancheCalculator.calculate(
                grade, pos.qty, pos.price, ctx.nav, ctx.targetRatios[cd], soldCycles(ctx, cd),
            )
            if (o.side != TrancheSide.NONE && o.qty > 0L) {
                candidates += OrderCandidate(cd, o.side, o.qty, grade, pos.price, nextCycleIndex(ctx, cd, o.side))
            }
        }

        // 신규 진입 — 비보유 + STRONG_BUY/BUY 만, 성향필터(정리매매·투자위험) 통과, 동시보유 ≤20 캡.
        // 우선순위: 등급(STRONG_BUY 먼저) → 동급 내 연기금 K 큰 순.
        // 만석이어도 이번 사이클 전량 매도 발행 수만큼은 슬롯이 열릴 예정 — 그만큼의 픽은 즉시 매수 대신
        // 슬롯대기 SKIP 으로 기록해 2차(09:01)에 넘긴다(2차가 보유 수 재조회로 실제 개방 확인 후 발행 —
        // 매도 미체결이면 자동 무산되어 캡 초과 없음).
        val newSlots = (MAX_CONCURRENT_HOLDINGS - ctx.holdings.size).coerceAtLeast(0)
        val pendingExitSlots = candidates.count {
            it.side == TrancheSide.SELL && it.qty == ctx.holdings[it.stkCd]?.qty
        }
        val newEntryCds = ctx.grades.keys
            .filter { it !in ctx.holdings && (ctx.grades[it] == "STRONG_BUY" || ctx.grades[it] == "BUY") }
            .sortedWith(
                compareBy({ if (ctx.grades[it] == "STRONG_BUY") 0 else 1 }, { -(ctx.pickK[it] ?: 0.0) })
            )
        val slotWaitSkips = mutableListOf<SkipCandidate>()
        var used = 0
        for (cd in newEntryCds) {
            if (used >= newSlots + pendingExitSlots) break
            if (cd in ctx.riskBlocked) continue              // ⑤-a 정리매매/투자위험 → 신규 진입 차단
            val price = ctx.seedPrices[cd] ?: continue       // ma5 없으면 사이징 불가 → 스킵
            if (price <= 0L) continue
            val grade = ctx.grades.getValue(cd)
            val o = trancheCalculator.calculate(grade, 0L, price, ctx.nav, ctx.targetRatios[cd])
            if (o.side == TrancheSide.BUY && o.qty > 0L) {
                if (used < newSlots) {
                    candidates += OrderCandidate(cd, o.side, o.qty, grade, price, cycleIndex = 1)
                } else {
                    slotWaitSkips += SkipCandidate(
                        cd, o.qty, price, grade, newEntry = true, cycleIndex = 1, slotWait = true,
                    )
                }
                used++
            }
        }
        return candidates to slotWaitSkips
    }

    /**
     * ⑤-b 거래정지 종목 코드 집합 (실시간 stockInfoList, KOSPI+KOSDAQ 2콜).
     * 보유 매도 포함 전 후보에 적용(거래정지면 못 팖). 조회 실패 시 빈 집합(fail-open,
     * 진짜 거래정지면 미체결→다음날 ① 취소로 자연 처리 — 격리).
     */
    private fun fetchHaltedCodes(): Set<String> {
        return try {
            val kospi = stockClient.stockInfoList(StockInfoListReq(mrkt_tp = "0")).list.orEmpty()
            Thread.sleep(ORDER_PACING_MS)
            val kosdaq = stockClient.stockInfoList(StockInfoListReq(mrkt_tp = "10")).list.orEmpty()
            (kospi + kosdaq)
                .filter { it.auditInfo == "거래정지" }
                .mapNotNull { normCd(it.code) }
                .toSet()
        } catch (e: Exception) {
            log.error(e) { "거래정지 조회 실패 — 빈 집합(fail-open), 미체결→다음날 취소로 자연 처리" }
            emptySet()
        }
    }

    /** 키움 종목코드 정규화 — "A005930"/"005930_AL" → "005930". HoldingGradeService 와 동일. */
    private fun normCd(raw: String?): String? =
        raw?.substringBefore("_")?.trimStart('A', 'a')?.ifBlank { null }

    /** 키움 숫자 문자열(부호·0패딩) → 절대값 Long. 파싱 실패 0. */
    private fun parseAmt(raw: String?): Long {
        val v = raw?.replace(Regex("[^0-9-]"), "")?.toLongOrNull() ?: return 0L
        return if (v < 0) -v else v
    }

    /**
     * 전날 잔여 미체결 전부 취소. 실패는 격리(로그 후 진행) — 일시 블립이 잡 전체를
     * 막지 않게(에러 로그는 모니터링 노출). 시장가라 미체결 자체가 드문 안전장치 단계.
     */
    private fun cancelPreviousUnfilled() {
        try {
            val res = kiwoomOrderClient.pendingOrders(
                KiwoomPendingOrderReq(all_stk_tp = "0", trde_tp = "0", stk_cd = null, stex_tp = "1")
            )
            val pending = res.oso.orEmpty()
            if (pending.isEmpty()) {
                log.info { "PaperTradeExecScheduler: 전날 미체결 없음" }
                return
            }
            val today = LocalDate.now()
            var cancelled = 0
            for (o in pending) {
                val ordNo = o.ord_no ?: continue
                val stkCd = o.stk_cd ?: continue
                // 키움 모의 kt10003 레이트리밋(1700) 회피 — pendingOrders 직후 첫 cancel 포함 매 호출 페이싱.
                Thread.sleep(ORDER_PACING_MS)
                try {
                    kiwoomOrderClient.cancelOrder(
                        KiwoomCancelOrderReq(
                            dmst_stex_tp = "KRX",
                            orig_ord_no = ordNo,
                            stk_cd = stkCd,
                            cncl_qty = "0", // 잔량 전부 취소
                        )
                    )
                    paperFillRepository.save(
                        PaperFill(
                            stkCd = stkCd,
                            side = "CNCL",
                            fillDate = today,
                            quantity = o.oso_qty?.toLongOrNull() ?: 0L,
                            price = 0L,
                            kiwoomOrderNo = ordNo,
                            note = "전날 미체결 취소(잔량전부)",
                        )
                    )
                    cancelled++
                } catch (e: Exception) {
                    log.error(e) { "미체결 취소 실패 ord_no=$ordNo stk_cd=$stkCd" }
                }
            }
            log.info { "PaperTradeExecScheduler: 전날 미체결 ${pending.size}건 중 $cancelled 건 취소" }
        } catch (e: Exception) {
            log.error(e) { "미체결 조회/취소 단계 실패 — 빈 것으로 간주하고 다음 단계 진행" }
        }
    }

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
