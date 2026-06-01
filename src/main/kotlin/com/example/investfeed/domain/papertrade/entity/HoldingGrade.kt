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

    @Column(name = "market_type")
    val marketType: String? = null, // KOSPI/KOSDAQ

    @Column(name = "eval_date", nullable = false)
    val evalDate: LocalDate,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    // HOLD 사유 — CONFLICT(BUY/SELL 양방향 시그널 충돌)일 때만 값 존재, 그 외 NULL.
    // 매매 결정에는 영향 없고, 사후 분석/디버깅 단서로 사용.
    @Column(name = "evaluation_reason")
    val evaluationReason: String? = null,
)
