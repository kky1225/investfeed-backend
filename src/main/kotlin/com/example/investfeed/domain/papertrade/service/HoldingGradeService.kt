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

/**
 * 보유 종목 등급 산출 서비스 — 매 거래일 22:10 (추천 22:00 직후, 독립 잡).
 *
 * 추천(컨텐츠) ≠ 보유평가(내 포지션에 행동 취하는 매매 시스템) 이므로 별도 스케줄.
 * **모든 보유 종목**을 [RecommendService.evaluateHoldingGrade] 로 평가(holdingMode=true →
 * BLOCK 한 단계 다운그레이드). 추천에 같이 잡힌 종목도 평가하여 holding_grade 에 별도 기록,
 * 매매 잡(09:00)이 보유 종목에 대해 holding_grade 를 stock_pick 보다 우선 적용함.
 * 보유 ∩ 추천 겹침 시 추천 백본의 보수적 HOLD 가 손절 시그널을 묻어버리는 문제 회피.
 * 등급은 사용자 독립 → 전 계좌 보유의 **distinct stk_cd 1회 평가**(다계좌여도 종목당 1회).
 *
 * 결과는 holding_grade(eval_date 당 1행 upsert) 에 영속 → 다음 거래일 09:00 실행 잡이 소비.
 * 매크로(동행지표)는 매매 경로 의도적 제외(evaluateHoldingGrade 내부에서 미적용).
 */
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
        // 휴일에 수동 트리거되는 경우(서버 다운 후 복구 등) eval_date 를 직전 거래일로 보정.
        // 평일 정상 실행 시엔 LocalDate.now() 그대로. RecommendService.doRecommendStock 과 동일 패턴.
        val evalDate = if (holidayService.isHoliday()) {
            holidayService.lastTradingDay()
        } else {
            LocalDate.now()
        }

        // 지수 종가 수집은 별도 잡(scheduledCollectIndexClose, 00:10)으로 분리.
        // 22:10 호출 시 키움이 당일 일봉 정산 전 응답을 줘 open/close 둘 다 부정확하게 들어가는 문제 때문.

        val held = fetchHeldHoldings()
        if (held.isEmpty()) {
            log.info { "HoldingGradeScheduler: 보유 종목 없음 — 평가 대상 0건 (eval_date=$evalDate)" }
            return
        }

        // 보유 종목 전체 평가(distinct, 종목당 1회). 추천에 같이 잡힌 종목도 holding_grade 에
        // 기록 → 09:00 매매 잡이 보유 종목에 대해 holding_grade 를 stock_pick 보다 우선 적용.
        // 추천 백본의 BLOCK→HOLD 가 손절 시그널을 묻어버리는 문제를 holdingMode=true(BLOCK→다운그레이드)로 회피.
        val targets = held.distinctBy { it.stkCd }

        var saved = 0
        for (h in targets) {
            try {
                val r = recommendService.evaluateHoldingGrade(h.stkCd, h.stkNm)
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

    /**
     * 키움 모의계좌 보유 종목 목록 (kt00018, mock-url). 보유수량>0 만.
     *
     * 모의계좌가 잔고 진실 소스. 조회 실패는 격리(로그 후 빈 리스트) — 지수 수집은 이미 끝났고,
     * 일시적 API 블립이 잡 전체를 실패로 만들지 않도록(에러 로그는 모니터링에 그대로 노출).
     * 첫 런은 계좌 0 보유 시작 → Phase 4 매매 전까지 빈 리스트가 정상.
     *
     * 종목코드 정규화: kt00018 응답은 "A005930" 형식 → 백본 평가(stockInvestor)와 맞추기 위해
     * 선행 영문 prefix 제거 + "_" 접미사 제거. (정확한 코드 포맷 정합은 Phase 4 드라이런에서 검증.)
     */
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
