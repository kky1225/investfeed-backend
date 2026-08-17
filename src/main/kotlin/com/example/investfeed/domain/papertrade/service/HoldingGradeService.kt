package com.example.investfeed.domain.papertrade.service

import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.papertrade.entity.HoldingGrade
import com.example.investfeed.domain.papertrade.repository.HoldingGradeRepository
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
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class HoldingGradeService(
    private val recommendService: RecommendService,
    private val holdingGradeRepository: HoldingGradeRepository,
    private val mockAccountClient: MockAccountClient,
    private val schedulerLogService: SchedulerLogService,
    private val authClient: AuthClient,
    private val holidayService: HolidayService,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {
    private val log = KotlinLogging.logger {}

    private data class HeldStock(val stkCd: String, val stkNm: String)

    @Transactional
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
                val r = recommendService.evaluateHoldingGrade(h.stkCd, h.stkNm, meta)
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
