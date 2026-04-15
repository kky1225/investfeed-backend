package com.example.investfeed.domain.holding.dto.res

import com.example.investfeed.domain.goal.dto.res.InvestmentGoalRes
import com.example.investfeed.domain.realizedpnl.dto.res.RealizedPnlDashboardItem

data class AssetDashboardRes(
    val totalAsset: Long,
    val totalEvltAmt: Long,
    val totalPurAmt: Long,
    val totalEvltPl: Long,
    val totalPrftRt: String,
    val totalCash: Long,
    val stockSummary: AssetGroupSummary,
    val cryptoSummary: AssetGroupSummary,
    val brokerSummaries: List<BrokerSummaryItem>,
    var realizedPnl: RealizedPnlDashboardItem? = null,
    var goals: List<InvestmentGoalRes>? = null,
)

data class AssetGroupSummary(
    val evltAmt: Long,
    val purAmt: Long,
    val evltPl: Long,
    val prftRt: String,
    val cash: Long,
    val ratio: String,
    val holdings: List<UnifiedHoldingItem>,
)

data class BrokerSummaryItem(
    val brokerName: String,
    val market: String,
    val type: String,
    val evltAmt: Long,
    val purAmt: Long,
    val evltPl: Long,
    val prftRt: String,
    val cash: Long,
    val holdingCount: Int,
    val holdings: List<BrokerHoldingItem>,
)

data class BrokerHoldingItem(
    val stkCd: String,
    val curPrc: String,
    val purAmt: Long,
    val quantity: Double,
)

data class UnifiedHoldingItem(
    val stkCd: String,
    val stkNm: String,
    val curPrc: String,
    val purAmt: Long,
    val evltAmt: Long,
    val evltPl: Long,
    val prftRt: String,
    val possRt: String,
    val brokerName: String,
)
