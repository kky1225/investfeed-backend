package com.example.investfeed.domain.papertrade.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 저녁 보유평가 잡이 산출한 종목별 등급 (eval_date 당 1행).
 * 다음 거래일 09:00 모의매매 실행 잡이 읽어 트랜치 매매에 사용.
 */
@Entity
@Table(
    name = "holding_grade",
    uniqueConstraints = [UniqueConstraint(columnNames = ["stk_cd", "eval_date"])]
)
class HoldingGrade(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "stk_cd", nullable = false)
    val stkCd: String,

    @Column(name = "stk_nm", nullable = false)
    val stkNm: String,

    @Column(name = "type", nullable = false)
    val type: String, // STRONG_BUY / BUY / HOLD / SELL / STRONG_SELL

    @Column(name = "origin_side")
    val originSide: String? = null, // classify originSide (BUY/SELL)

    @Column(name = "penfnd_k")
    val penfndK: Double? = null,

    @Column(name = "frgnr_mcap_ratio")
    val frgnrMcapRatio: Double? = null,

    // ─── 결정 근거 (왜 이 등급/비중/사유인지 추적용) ───
    @Column(name = "frgnr_opposite_k")
    val frgnrOppositeK: Double? = null,    // 외국인 반대 K (BLOCK·freeze·부분비중 강도 근거)

    @Column(name = "frgnr_same_dir_k")
    val frgnrSameDirK: Double? = null,     // 외국인 동조 K (하드스톱 근거)

    @Column(name = "prior_trend_ratio")
    val priorTrendRatio: Double? = null,   // B′ 연기금 prior 추세 명확성 (STRONG 근거)

    @Column(name = "foreigner_aligned")
    val foreignerAligned: Boolean? = null, // 옵션B 외국인 12일 동조

    @Column(name = "market_type")
    val marketType: String? = null, // KOSPI/KOSDAQ

    @Column(name = "eval_date", nullable = false)
    val evalDate: LocalDate,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    // HARD_SELL / BLOCK_FREEZE / BLOCK_PARTIAL / MODULE_HALF / FLOW_BUY/FLOW_SELL(수급 지속 매집/분배) / CONFLICT (복수면 '|' 결합), 없으면 NULL.
    @Column(name = "evaluation_reason")
    val evaluationReason: String? = null,

    // 외국인 BLOCK 중간반대(1.5~3.0) 시 0.10(부분 트림/매수), 그 외 NULL(기본 매수 20%/매도 0%).
    @Column(name = "target_weight_ratio")
    val targetWeightRatio: Double? = null,

    @Column(name = "pre_adjustment_type")
    val preAdjustmentType: String? = null,

    @Column(name = "backbone_reason")
    val backboneReason: String? = null,

    @Column(name = "pv_trigger")
    val pvTrigger: String? = null,

    @Column(name = "ma_trigger")
    val maTrigger: String? = null,

    @Column(name = "vp_trigger")
    val vpTrigger: String? = null,

    @Column(name = "rsi_trigger")
    val rsiTrigger: String? = null,

    @Column(name = "hl52w_trigger")
    val hl52wTrigger: String? = null,

    @Column(name = "breakout_trigger")
    val breakoutTrigger: String? = null,

    // MA5/MA20 교차 나이(거래일, 교차 당일=1). null=교차 미관측(낡음 취급) — MovingAverageModule 신선도 게이트(≤5)
    @Column(name = "ma_cross_age")
    val maCrossAge: Int? = null,
)
