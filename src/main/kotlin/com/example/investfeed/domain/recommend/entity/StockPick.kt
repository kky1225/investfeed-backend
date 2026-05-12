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
)
