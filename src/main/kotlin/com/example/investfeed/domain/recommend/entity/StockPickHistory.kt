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

    @Column(name = "pick_date", nullable = false)
    val pickDate: LocalDateTime,
)
