package com.example.investfeed.domain.recommend.admin.dto.res

import java.time.LocalDate

data class AdminMarketSnapshotRes(
    val capturedDate: LocalDate,

    // KOSPI
    val kospiChangeRate: Double?,
    val kospiForeignerSign: String?,        // BUY / SELL / NEUTRAL
    val kospiInstitutionSign: String?,
    val kospiScenario: String?,             // UP_BUY_BUY, DOWN_FULL_SELL, NEUTRAL 등

    // KOSDAQ
    val kosdaqChangeRate: Double?,
    val kosdaqForeignerSign: String?,
    val kosdaqInstitutionSign: String?,
    val kosdaqScenario: String?,
)
