package com.example.investfeed.domain.papertrade.dto.res

import java.time.LocalDate

data class PaperTradeReportRes(
    val startDate: LocalDate?,
    val startNav: Long,
    val currentNav: Long?,
    val totalReturnPct: Double?,
    val kospiReturnPct: Double?,
    val kosdaqReturnPct: Double?,
    val blendedBenchmarkPct: Double,
)
