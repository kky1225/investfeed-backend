package com.example.investfeed.domain.recommend.entity

enum class RiskCategory(val matches: (StockPick) -> Boolean) {
    DELISTING({ it.isDelisting == true }),
    INVESTMENT_RISK({ it.isInvestmentRisk == true }),
    OVERHEATED({ it.isOverheated == true }),
    INVESTMENT_WARNING({ it.isInvestmentWarning == true }),
    INVESTOR_ALERT({ it.isInvestorAlert == true }),
    MANAGED({ it.isManaged == true }),
}