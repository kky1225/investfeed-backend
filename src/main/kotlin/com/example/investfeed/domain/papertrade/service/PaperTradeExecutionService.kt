package com.example.investfeed.domain.papertrade.service

import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.papertrade.entity.PaperFill
import com.example.investfeed.domain.papertrade.repository.HoldingGradeRepository
import com.example.investfeed.domain.papertrade.repository.PaperFillRepository
import com.example.investfeed.domain.recommend.entity.RiskPreset
import com.example.investfeed.domain.recommend.repository.StockPickRepository
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
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 모의 매매 실행 잡 — 매 거래일 **08:50 장전**.
 *
 * 09:00 정각 시작은 시가를 못 잡음(장 시작 동시호가 08:30~09:00). 08:50에 prep 끝내고
 * 시장가 주문을 동시호가 창에 제출 → 09:00 시가 체결(백테스트 price_open_1d 와 정합).
 *
 * 처리 순서(고정): ① 전날 미체결 취소 → ② 예수금·보유 재조회 → ③ 등급 로드 → ④ 트랜치 →
 * ⑤ 사전필터(⑤-a 신규진입 성향=NORMAL 정리매매·투자위험 / ⑤-b 거래정지 실시간 전후보)
 * → ⑥ 현금 STRONG우선 배분 → ⑦ 시장가 동시호가 제출 → ⑧ paper_fill 행동 로그.
 *
 * 상하한 사전필터 없음(장전 판정 불가, 미체결→다음날 ① 취소로 자연 처리). 실제 체결가는
 * 시가(09:00)라 paper_fill엔 사이징가+메모만 기록(키움 모의계좌가 잔고 진실 소스).
 */
@Service
class PaperTradeExecutionService(
    private val kiwoomOrderClient: KiwoomOrderClient,
    private val mockAccountClient: MockAccountClient,
    private val stockClient: StockClient,
    private val trancheCalculator: TrancheCalculator,
    private val paperFillRepository: PaperFillRepository,
    private val holdingGradeRepository: HoldingGradeRepository,
    private val stockPickRepository: StockPickRepository,
    private val holidayService: HolidayService,
    private val schedulerLogService: SchedulerLogService,
    private val authClient: AuthClient,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val ORDER_PACING_MS = 1500L  // 종목 루프 키움 호출 페이싱(모의 주문 레이트리밋 회피, 500→1500)
    }

    /** 한 종목의 현재 보유 상태(가격은 사이징·환산용). */
    private data class HeldPos(val qty: Long, val price: Long)

    /** ②③ 결과 — 트랜치/주문(서브스텝 3·4)의 입력. */
    private data class ExecContext(
        val nav: Long,                        // 총 평가액 (현금 + 보유평가)
        val availableCash: Long,              // 주문가능금액
        val holdings: Map<String, HeldPos>,   // 정규화 stkCd → 보유
        val grades: Map<String, String>,      // 정규화 stkCd → 등급
        val seedPrices: Map<String, Long>,    // 정규화 stkCd → 신규진입 사이징가(ma5)
        val riskBlocked: Set<String>,         // ⑤-a NORMAL 차단(정리매매·투자위험) — 신규진입만 적용
    )

    /** ④ 산출 — 종목별 희망 주문(시장가). 사전필터·현금배분·발행은 서브스텝 4. */
    private data class OrderCandidate(
        val stkCd: String,
        val side: TrancheSide,
        val qty: Long,
        val grade: String,
        val price: Long,   // 사이징/현금배분 환산가(시장가라 실제 체결가는 시가)
    )

    @Scheduled(cron = "0 50 8 * * *", scheduler = "slowScheduler")
    fun scheduledPaperTradeExec() {
        log.info { "PaperTradeExecScheduler cron fired" }
        if (holidayService.isHoliday()) {
            log.info { "PaperTradeExecScheduler skipped: today is holiday" }
            return
        }
        runPaperTradeExec()
    }

    @Transactional
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

        // ④ 종목별 희망 주문 산출
        val candidates = buildOrderCandidates(ctx)
        log.info {
            "PaperTradeExecScheduler 주문후보 ${candidates.size}건: " +
                candidates.joinToString { "${it.stkCd}:${it.side}:${it.qty}(${it.grade})" }
        }

        // ⑤-b 거래정지 제외 (보유 매도 포함 전 후보). ⑤-a(성향필터)는 buildOrderCandidates에서 적용됨.
        val halted = fetchHaltedCodes()
        val filtered = candidates.filter { it.stkCd !in halted }
        if (filtered.size != candidates.size) {
            log.info { "PaperTradeExecScheduler: 거래정지 ${candidates.size - filtered.size}건 제외" }
        }

        // ⑥ 현금 STRONG우선 배분: 매도는 전부 발행, 매수는 STRONG_BUY 우선 + 가용현금 한도까지
        val sells = filtered.filter { it.side == TrancheSide.SELL }
        val buys = filtered.filter { it.side == TrancheSide.BUY }
            .sortedBy { if (it.grade == "STRONG_BUY") 0 else 1 }
        val fundedBuys = mutableListOf<OrderCandidate>()
        var cashLeft = ctx.availableCash
        for (b in buys) {
            val cost = b.qty * b.price
            if (cost <= cashLeft) {
                fundedBuys += b
                cashLeft -= cost
            } else {
                log.info { "PaperTradeExecScheduler: 현금부족 스킵 ${b.stkCd}(${b.grade}) cost=$cost left=$cashLeft" }
            }
        }
        val toOrder = sells + fundedBuys

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
                        note = "시장가 동시호가 제출(등급=${c.grade}, 사이징가=${c.price})",
                    )
                )
                ok++
            } catch (e: Exception) {
                log.error(e) { "주문 실패 ${c.stkCd} ${c.side} ${c.qty}" }
                paperFillRepository.save(
                    PaperFill(
                        stkCd = c.stkCd, side = c.side.name, fillDate = today,
                        quantity = c.qty, price = c.price, kiwoomOrderNo = null,
                        note = "주문거부/실패: ${e.message?.take(180)}",
                    )
                )
            }
        }
        log.info {
            "PaperTradeExecScheduler 완료 — 후보 ${candidates.size}, 발행대상 ${toOrder.size}" +
                "(매도 ${sells.size}/매수 ${fundedBuys.size}), 성공 $ok"
        }
    }

    /**
     * ② 키움 모의계좌 예수금·보유 재조회 + ③ 등급 로드(직전 거래일 holding_grade + 당일 stock_pick).
     *
     * 키움 모의계좌가 잔고 진실 소스. NAV = 추정예탁자산(없으면 현금+총평가).
     * **보유 종목은 holding_grade 우선, 신규 매수 후보는 stock_pick 우선.** 보유 ∩ 추천 겹침 시
     * 추천 백본의 보수적 HOLD 가 손절 시그널을 묻어버리는 문제 회피(holding_grade 는 holdingMode=true
     * 로 BLOCK→다운그레이드 처리). 코드 정규화는 HoldingGradeService 와 동일 규칙.
     */
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
        val seedPrices = mutableMapOf<String, Long>()
        val riskBlocked = mutableSetOf<String>()
        val normalBlocked = RiskPreset.NORMAL.blockedCategories() // {정리매매, 투자위험}
        stockPickRepository.findAll().forEach { p ->
            val cd = normCd(p.stkCd) ?: return@forEach
            grades[cd] = p.type
            // 신규 진입 사이징가 = ma5(저장값, 라이브 호출 X). 어차피 시장가 시가 체결이라 근사 충분.
            p.ma5?.takeIf { it > 0 }?.let { seedPrices[cd] = it.toLong() }
            // ⑤-a 성향필터: NORMAL=정리매매/투자위험 종목은 신규 진입 차단(보유는 면제 — 별도 적용)
            if (normalBlocked.any { it.matches(p) }) riskBlocked += cd
        }
        holdingGradeRepository.findByEvalDate(priorTradingDay).forEach { g ->
            val cd = normCd(g.stkCd) ?: return@forEach
            if (cd in holdings) grades[cd] = g.type
        }
        return ExecContext(
            nav = nav, availableCash = availableCash,
            holdings = holdings, grades = grades, seedPrices = seedPrices,
            riskBlocked = riskBlocked,
        )
    }

    /**
     * ④ 종목별 희망 주문 산출 (TrancheCalculator).
     * - 보유 종목: grades 등급(없으면 HOLD=동결)으로 평가, 가격=조회 현재가.
     * - 비보유 종목: stock_pick STRONG_BUY/BUY 만 신규 진입 후보(HOLD/SELL 등은 무행동 — 없는 걸 못 팖).
     *   가격=ma5(없으면 스킵). 동시 보유 ≤10 캡(신규 진입 한정), STRONG_BUY 우선.
     * 현금 STRONG우선 배분·거래정지/상하한 사전필터·실제 발행은 서브스텝 4.
     */
    private fun buildOrderCandidates(ctx: ExecContext): List<OrderCandidate> {
        val candidates = mutableListOf<OrderCandidate>()

        // 보유 종목 — 등급대로(HOLD/SELL/STRONG_SELL 등 전부)
        for ((cd, pos) in ctx.holdings) {
            val grade = ctx.grades[cd] ?: "HOLD"
            val o = trancheCalculator.calculate(grade, pos.qty, pos.price, ctx.nav)
            if (o.side != TrancheSide.NONE && o.qty > 0L) {
                candidates += OrderCandidate(cd, o.side, o.qty, grade, pos.price)
            }
        }

        // 신규 진입 — 비보유 + STRONG_BUY/BUY 만, 성향필터(정리매매·투자위험) 통과, 동시보유 ≤10 캡, STRONG 우선
        val newSlots = (10 - ctx.holdings.size).coerceAtLeast(0)
        val newEntryCds = ctx.grades.keys
            .filter { it !in ctx.holdings && (ctx.grades[it] == "STRONG_BUY" || ctx.grades[it] == "BUY") }
            .sortedBy { if (ctx.grades[it] == "STRONG_BUY") 0 else 1 }
        var used = 0
        for (cd in newEntryCds) {
            if (used >= newSlots) break
            if (cd in ctx.riskBlocked) continue              // ⑤-a 정리매매/투자위험 → 신규 진입 차단
            val price = ctx.seedPrices[cd] ?: continue       // ma5 없으면 사이징 불가 → 스킵
            if (price <= 0L) continue
            val grade = ctx.grades.getValue(cd)
            val o = trancheCalculator.calculate(grade, 0L, price, ctx.nav)
            if (o.side == TrancheSide.BUY && o.qty > 0L) {
                candidates += OrderCandidate(cd, o.side, o.qty, grade, price)
                used++
            }
        }
        return candidates
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
