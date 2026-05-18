package com.example.investfeed.domain.recommend.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "stock_pick_history")
class StockPickHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val type: String,

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

    @Column(name = "pick_price")
    val pickPrice: Long? = null,

    @Column(name = "market_cap")
    val marketCap: Long? = null,

    @Column(name = "origin_side")
    val originSide: String? = null,

    @Column(name = "today_direction")
    val todayDirection: String? = null,

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

    // VolumePriceModule 평가용
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
    val high52w: Long? = null,

    @Column(name = "low52w")
    val low52w: Long? = null,

    @Column(name = "dist_from_high_52w")
    val distFromHigh52w: Double? = null,

    @Column(name = "dist_from_low_52w")
    val distFromLow52w: Double? = null,

    @Column(name = "close_above_ma20")
    val closeAboveMa20: Boolean? = null,

    // 백테스트/디버깅용 — 각 후행 모듈 트리거 결과 ('PROMOTE' / 'DEMOTE' / 'NONE').
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
    val hl52wTrigger: String? = null,

    @Column(name = "breakout_trigger")
    val breakoutTrigger: String? = null,

    // N영업일 후 가격 (BacktestBackfillService 가 백필).
    // T+1일 시가 매수 / T+N일 종가 평가 백테스트 모델 기준.
    @Column(name = "price_open_1d")
    var priceOpen1d: Long? = null,

    @Column(name = "price_close_1d")
    var priceClose1d: Long? = null,

    @Column(name = "price_close_5d")
    var priceClose5d: Long? = null,

    @Column(name = "price_close_20d")
    var priceClose20d: Long? = null,

    @Column(name = "pick_date", nullable = false)
    val pickDate: LocalDateTime,
)
