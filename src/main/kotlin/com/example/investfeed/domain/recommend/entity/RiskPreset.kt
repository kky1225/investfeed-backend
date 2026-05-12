package com.example.investfeed.domain.recommend.entity

enum class RiskPreset {
    AGGRESSIVE,
    NORMAL,
    CONSERVATIVE;

    fun blockedCategories(): Set<RiskCategory> = when (this) {
        AGGRESSIVE -> emptySet()
        NORMAL -> setOf(RiskCategory.DELISTING, RiskCategory.INVESTMENT_RISK)
        CONSERVATIVE -> setOf(
            RiskCategory.DELISTING,
            RiskCategory.INVESTMENT_RISK,
            RiskCategory.OVERHEATED,
            RiskCategory.INVESTMENT_WARNING,
            RiskCategory.INVESTOR_ALERT,
            RiskCategory.MANAGED,
        )
    }
}