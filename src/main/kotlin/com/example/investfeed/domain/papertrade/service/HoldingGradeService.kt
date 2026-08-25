package com.example.investfeed.domain.papertrade.service

import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.papertrade.entity.HoldingGrade
import com.example.investfeed.domain.papertrade.entity.PaperFill
import com.example.investfeed.domain.papertrade.repository.HoldingGradeRepository
import com.example.investfeed.domain.papertrade.repository.PaperFillRepository
import com.example.investfeed.domain.recommend.service.RecommendService
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomHoldingReq
import com.example.investfeed.kiwoom.order.client.MockAccountClient
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class HoldingGradeService(
    private val recommendService: RecommendService,
    private val holdingGradeRepository: HoldingGradeRepository,
    private val paperFillRepository: PaperFillRepository,
    private val mockAccountClient: MockAccountClient,
    private val schedulerLogService: SchedulerLogService,
    private val authClient: AuthClient,
    private val holidayService: HolidayService,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {
    private val log = KotlinLogging.logger {}

    private data class HeldStock(val stkCd: String, val stkNm: String)

    companion object {
        private val FILL_SIDES = listOf("BUY", "SELL")

        /**
         * 현재 포지션의 진입일 — 체결 이력(시간순)을 누적해 순보유량이 0 → 양수로 바뀐 **가장 최근** 시점.
         * 최초 BUY 일자를 쓰면 전량 매도 후 재매수한 종목의 이전 보유 기간이 섞이므로(실례 000500) 재진입을 따라간다.
         * paper_fill 은 주문 제출 기록이라 미체결분이 섞일 수 있으나, 미체결은 익일 취소돼 실제 순보유와 수렴한다.
         */
        internal fun currentEntryDate(fills: List<PaperFill>): LocalDate? {
            var qty = 0L
            var entry: LocalDate? = null
            for (f in fills) {
                val before = qty
                qty += when (f.side) {
                    "BUY" -> f.quantity
                    "SELL" -> -f.quantity
                    else -> 0L
                }
                if (before <= 0L && qty > 0L) entry = f.fillDate
                if (qty <= 0L) { qty = 0L; entry = null }
            }
            return entry
        }
    }

    // 메서드 단위 @Transactional 금지: 종목당 외부 API 호출(수 초)을 DB 트랜잭션 하나에 묶으면
    // ① delete 가 flush 까지 지연되어 같은 (stk_cd, eval_date) 재평가 시 INSERT 가 DELETE 보다 먼저 실행돼 유니크 위반
    // ② 한 종목의 예외가 세션을 오염시켜 나머지 종목까지 전부 실패(per-stock try/catch 무효화).
    // repository 호출이 각각 커밋되어야 delete→save 순서와 종목별 격리가 보장된다.
    fun runHoldingGrade() {
        schedulerLogService.execute(SchedulerName.HoldingGradeScheduler) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
                doHoldingGrade()
            } finally {
                SecurityContextHolder.clearContext()
            }
        }
    }

    private fun doHoldingGrade() {
        val evalDate = if (holidayService.isHoliday()) {
            holidayService.lastTradingDay()
        } else {
            LocalDate.now()
        }

        val held = fetchHeldHoldings()
        if (held.isEmpty()) {
            log.info { "HoldingGradeScheduler: 보유 종목 없음 — 평가 대상 0건 (eval_date=$evalDate)" }
            return
        }

        val targets = held.distinctBy { it.stkCd }

        val meta = recommendService.buildStockMetadataMaps()

        var saved = 0
        for (h in targets) {
            try {
                // 지속 매집 판정의 창 시작점 — 현재 포지션의 진입일(전량 매도 후 재매수면 재진입일)
                val entryDate = currentEntryDate(
                    paperFillRepository.findByStkCdAndSideInOrderByFillDateAscIdAsc(h.stkCd, FILL_SIDES)
                )
                val r = recommendService.evaluateHoldingGrade(h.stkCd, h.stkNm, meta, entryDate, evalDate)
                holdingGradeRepository.findByStkCdAndEvalDate(h.stkCd, evalDate)
                    ?.let { holdingGradeRepository.delete(it) }
                holdingGradeRepository.save(
                    HoldingGrade(
                        stkCd = r.stkCd,
                        stkNm = r.stkNm,
                        type = r.type,
                        originSide = r.originSide,
                        penfndK = r.penfndK,
                        frgnrMcapRatio = r.frgnrMcapRatio,
                        frgnrOppositeK = r.frgnrOppositeK,
                        frgnrSameDirK = r.frgnrSameDirK,
                        priorTrendRatio = r.priorTrendRatio,
                        foreignerAligned = r.foreignerAligned,
                        marketType = r.marketType,
                        evalDate = evalDate,
                        evaluationReason = r.evaluationReason,
                        targetWeightRatio = r.targetWeightRatio,
                        preAdjustmentType = r.preAdjustmentType,
                        backboneReason = r.backboneReason,
                        pvTrigger = r.pvTrigger,
                        maTrigger = r.maTrigger,
                        vpTrigger = r.vpTrigger,
                        rsiTrigger = r.rsiTrigger,
                        hl52wTrigger = r.hl52wTrigger,
                        breakoutTrigger = r.breakoutTrigger,
                        maCrossAge = r.maCrossAge,
                    )
                )
                saved++
            } catch (e: Exception) {
                log.error(e) { "HoldingGradeScheduler 평가 실패 stkCd=${h.stkCd}" }
            }
        }
        log.info {
            "HoldingGradeScheduler 완료 — eval_date=$evalDate, 보유 distinct=${targets.size}, 저장=$saved"
        }
    }

    private fun fetchHeldHoldings(): List<HeldStock> {
        return try {
            val res = mockAccountClient.holdingList(
                KiwoomHoldingReq(qry_tp = "2", dmst_stex_tp = "KRX")
            )
            res?.acnt_evlt_remn_indv_tot.orEmpty()
                .filter { (it.rmnd_qty?.toLongOrNull() ?: 0L) > 0L }
                .mapNotNull { h ->
                    val rawCd = h.stk_cd ?: return@mapNotNull null
                    val stkCd = rawCd.substringBefore("_").trimStart('A', 'a').ifBlank { return@mapNotNull null }
                    HeldStock(stkCd = stkCd, stkNm = h.stk_nm ?: stkCd)
                }
        } catch (e: Exception) {
            log.error(e) { "HoldingGradeScheduler: 모의계좌 보유 조회 실패 — 빈 리스트로 진행" }
            emptyList()
        }
    }

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
