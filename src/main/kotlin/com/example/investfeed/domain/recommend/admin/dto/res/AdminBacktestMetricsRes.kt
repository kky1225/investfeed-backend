package com.example.investfeed.domain.recommend.admin.dto.res

/**
 * 추천 시스템 백테스트 — Aggregate Metrics.
 *
 * 표준 백테스트 KPI:
 *   - 평가 가능 표본 수 (price_close_Nd NOT NULL 인 row 수)
 *   - 평균 수익률 (mean return)
 *   - 적중률 (hit rate) — BUY 진영은 ret > 0 비율, SELL 진영은 ret < 0 비율
 *   - 표준편차 (변동성 지표)
 *   - 분해 (시나리오/등급/진영/모듈 trigger 별) 평균 수익률
 *
 * 데이터 부족 시 [insufficientReason] 으로 안내.
 */
data class AdminBacktestMetricsRes(
    val periodDays: Int,
    val totalSignals: Int,
    val insufficientReason: String?,

    val metrics1d: HorizonMetrics,
    val metrics5d: HorizonMetrics,
    val metrics20d: HorizonMetrics,

    val byScenario: List<GroupMetrics>,    // 매크로 시나리오별 (kospi_scenario 기준)
    val byType: List<GroupMetrics>,        // 등급별 (typeWithMacro)
    val byOriginSide: List<GroupMetrics>,  // 진영별 (BUY / SELL)
    val byModuleTrigger: List<GroupMetrics>, // 모듈 trigger 패턴별 (PV/MA/VP/RSI 조합)
)

data class HorizonMetrics(
    val horizon: String,               // "1d" / "5d" / "20d"
    val evaluable: Int,                // 평가 가능 표본 수 (N일 후 가격 백필 완료)
    val meanReturn: Double?,           // 평균 수익률 (%)
    val hitRate: Double?,              // 적중률 (BUY 진영=양수 비율 / SELL 진영=음수 비율)
    val stdDev: Double?,               // 수익률 표준편차
    val maxReturn: Double?,            // 최대 수익률
    val minReturn: Double?,            // 최소 수익률
)

data class GroupMetrics(
    val groupKey: String,              // 분해 키 (예: "UP_BUY_BUY", "STRONG_BUY", "BUY", "PV=PROMOTE,MA=NONE,...")
    val count: Int,
    val evaluable5d: Int,              // 5d 기준 평가 가능 수
    val meanReturn5d: Double?,         // 5d 평균 수익률
    val hitRate5d: Double?,            // 5d 적중률
)
