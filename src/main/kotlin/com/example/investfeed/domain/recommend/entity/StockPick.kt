package com.example.investfeed.domain.recommend.entity

import jakarta.persistence.*

@Entity
@Table(name = "stock_pick")
class StockPick(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val type: String, // STRONG_BUY / BUY / HOLD / SELL / STRONG_SELL

    @Column(nullable = false)
    val stkCd: String,

    @Column(nullable = false)
    val stkNm: String,

    @Column(name = "market_type")
    val marketType: String? = null,

    @Column(name = "penfnd_k")
    val penfndK: Double? = null,

    @Column(name = "frgnr_blocked")
    val frgnrBlocked: Boolean? = null,

    @Column(name = "frgnr_mcap_ratio")
    val frgnrMcapRatio: Double? = null,

    @Column(name = "origin_side")
    val originSide: String? = null,

    @Column(name = "today_direction")
    var todayDirection: String? = null,

    @Column(name = "is_managed")
    val isManaged: Boolean? = null,

    @Column(name = "is_delisting")
    val isDelisting: Boolean? = null,

    @Column(name = "is_overheated")
    val isOverheated: Boolean? = null,

    @Column(name = "is_investment_risk")
    val isInvestmentRisk: Boolean? = null,

    @Column(name = "is_investment_warning")
    val isInvestmentWarning: Boolean? = null,

    @Column(name = "is_investor_alert")
    val isInvestorAlert: Boolean? = null,

    @Column(name = "is_trading_halted")
    val isTradingHalted: Boolean? = null,

    @Column(name = "flu5_pct")
    val flu5Pct: Double? = null,

    @Column(name = "ma5")
    val ma5: Double? = null,

    @Column(name = "ma20")
    val ma20: Double? = null,

    // VolumePriceModule 평가용 - 22:00 스케줄러가 일봉 차트로 계산해 저장
    @Column(name = "avg_20d_volume")
    val avg20dVolume: Long? = null,

    @Column(name = "today_change_rate")
    val todayChangeRate: Double? = null,

    @Column(name = "today_volume")
    val todayVolume: Long? = null,

    // RsiModule 평가용
    @Column(name = "rsi14")
    val rsi14: Double? = null,

    @Column(name = "rsi14_breakdown_70")
    val rsi14Breakdown70: Boolean? = null,

    // HighLow52wModule 평가용 — Stage Analysis 기반 52주 가격 위치
    @Column(name = "high52w")
    val high52w: Long? = null,                  // 240영업일 최고 종가

    @Column(name = "low52w")
    val low52w: Long? = null,                   // 240영업일 최저 종가

    @Column(name = "dist_from_high_52w")
    val distFromHigh52w: Double? = null,        // % (음수, 고점 대비 하락률)

    @Column(name = "dist_from_low_52w")
    val distFromLow52w: Double? = null,         // % (양수, 저점 대비 상승률)

    @Column(name = "close_above_ma20")
    val closeAboveMa20: Boolean? = null,        // 종가 > MA20 (추세 위치)

    // 백테스트/디버깅용 — 각 후행 모듈 트리거 결과 ('PROMOTE' / 'DEMOTE' / 'NONE').
    // 22:00 시스템 디폴트 (모든 후행 모듈 ON) 기준 raw 평가 결과 (만장일치 룰 적용 전).
    // 매크로 트리거는 저장 X — 동행지표라 시간 lag 시 의미 변질 (운영 시 실시간만 적용).
    @Column(name = "pv_trigger")
    val pvTrigger: String? = null,

    @Column(name = "ma_trigger")
    val maTrigger: String? = null,

    @Column(name = "vp_trigger")
    val vpTrigger: String? = null,

    @Column(name = "rsi_trigger")
    val rsiTrigger: String? = null,

    @Column(name = "hl52w_trigger")
    val hl52wTrigger: String? = null,           // HighLow52w 트리거 (누락 보강)

    @Column(name = "breakout_trigger")
    val breakoutTrigger: String? = null,        // Breakout 트리거 (신규)
)
