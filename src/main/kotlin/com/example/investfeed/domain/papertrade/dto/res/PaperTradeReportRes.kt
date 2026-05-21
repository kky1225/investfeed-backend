package com.example.investfeed.domain.papertrade.dto.res

import java.time.LocalDate

/**
 * 모의 매매 성과 리포트.
 * - paper = 키움 모의계좌 NAV 기준 전체 수익(잔고 진실 소스).
 * - benchmark = index_daily_close 운용기간 수익(시장배분 정밀가중은 lot 폐기로 불가 → 코스피/코스닥 단순평균 근사).
 */
data class PaperTradeReportRes(
    val startDate: LocalDate?,
    val startNav: Long,
    val currentNav: Long,
    val totalReturnPct: Double,
    val kospiReturnPct: Double?,
    val kosdaqReturnPct: Double?,
    val blendedBenchmarkPct: Double,
)
