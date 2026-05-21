package com.example.investfeed.domain.calendar.dto.res

data class EconomicIndicatorsRes(
    val indicators: List<EconomicIndicator>,
    val lastUpdated: String? = null
)