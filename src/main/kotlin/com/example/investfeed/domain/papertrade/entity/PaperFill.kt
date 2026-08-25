package com.example.investfeed.domain.papertrade.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 모의 매매 엔진 행동·감사 로그 (디버깅·키움 대조용, 측정엔 미사용).
 * 신뢰 데이터는 키움 모의계좌 — 이건 "엔진이 무엇을 주문/체결했나" 추적용.
 * (등급 귀속·FIFO 손익 분리는 폐기 — 신호 품질은 백테스트가 담당.)
 */
@Entity
@Table(name = "paper_fill")
class PaperFill(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "stk_cd", nullable = false)
    val stkCd: String,

    @Column(name = "side", nullable = false)
    val side: String, // BUY / SELL / SKIP(현금부족·슬롯대기 미발행, 2차 매수 입력) / CNCL(전날 미체결 취소) / REJ(주문거부·실패, 체결 아님)

    @Column(name = "fill_date", nullable = false)
    val fillDate: LocalDate,

    @Column(name = "quantity", nullable = false)
    val quantity: Long,

    @Column(name = "price", nullable = false)
    val price: Long, // 체결단가 (원)

    @Column(name = "kiwoom_order_no")
    val kiwoomOrderNo: String? = null,

    @Column(name = "cycle_index")
    val cycleIndex: Int? = null, // 트랜치 사이클 번호

    @Column(name = "grade")
    val grade: String? = null, // 발행/스킵 시점 등급 (2차 우선순위·순수/모듈 성과 측정용)

    @Column(name = "new_entry")
    val newEntry: Boolean? = null, // 신규진입 여부 (매수/스킵 행)

    @Column(name = "phase")
    val phase: Int? = null, // 1=1차(08:50), 2=2차(09:01) — 2차 잔여 산출의 차감 기준

    @Column(name = "note")
    val note: String? = null, // 거부 사유/체결 메모 등

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
