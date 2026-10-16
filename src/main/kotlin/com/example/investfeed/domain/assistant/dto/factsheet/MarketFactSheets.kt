package com.example.investfeed.domain.assistant.dto.factsheet

import java.time.LocalDate
import java.time.LocalDateTime

sealed interface MarketFactSheet

data class QuoteFact(
    val price: Double,
    val changeRate: Double? = null,
    val changeAmount: Double? = null,
    val delayStatus: String? = null,
    val staleAt: LocalDateTime? = null,   // 수집 후 10분 넘게 갱신되지 않은 값이면 그 마지막 갱신 시각 (브리핑에 "몇 시 기준" 표기)
)

data class IndexFact(
    val name: String,
    val close: Double,
    val changeRate: Double?,
    val changeAmount: Double?,
    val high: Double? = null,
    val low: Double? = null,
    val tradeAmount: Long? = null,
    val high250: Double? = null,
    val low250: Double? = null,
    val delayStatus: String? = null,
)

data class MarketFlowFact(
    val foreign: Long?,
    val institution: Long?,
    val individual: Long?,
    val foreignStreak: Int = 0,
)

data class FlowFact(val kospi: MarketFlowFact?, val kosdaq: MarketFlowFact?)

data class SectorFact(val name: String, val changeRate: Double, val tradeAmount: Long?)

data class MacroFact(
    val usdKrw: QuoteFact?,
    val dollarIndex: QuoteFact?,
    val gold: QuoteFact?,
    val wti: QuoteFact?,
)

data class CoinDayFact(
    val market: String,
    val name: String,
    val close: Double,
    val prevClose: Double?,
    val changeRate: Double?,
    val tradeAmount: Double?,
)

data class KrHoldingsFactSheet(val tradeDate: LocalDate, val asOf: LocalDateTime) : MarketFactSheet

data class CoinDailyFactSheet(
    val tradeDate: LocalDate,
    val asOf: LocalDateTime,
    val btc: CoinDayFact?,
    val eth: CoinDayFact?,
    val fearGreed: Int?,
) : MarketFactSheet

data class TreasuryFact(
    val y2: Double?, val y2Bp: Double?,
    val y5: Double? = null, val y5Bp: Double? = null,
    val y10: Double?, val y10Bp: Double?,
    val y30: Double? = null, val y30Bp: Double? = null,
    val spreadBp: Int?,
    val source: String,
    val asOfDate: LocalDate,
    val stale: Boolean = false,
    val bondHoliday: Boolean = false,   // 미국 채권시장 휴장일 (주식시장은 개장)
)

data class CircuitBreakerFact(
    val name: String,             // 코스피 / 코스닥
    val stage: Int,               // 1~3
    val triggerRate: Double,
    val firedAt: LocalDateTime,
    val resumeAt: LocalDateTime?, // 3단계는 null (당일 장 종료)
)

data class KrCloseFactSheet(
    val tradeDate: LocalDate,
    val asOf: LocalDateTime,
    val kospi: IndexFact?,
    val kosdaq: IndexFact?,
    val kospi200: IndexFact? = null,
    val kosdaq150: IndexFact? = null,
    val kospiRate5d: Double? = null,
    val kosdaqRate5d: Double? = null,
    val flow: FlowFact?,
    val sectors: List<SectorFact>?,
    val usdKrw: QuoteFact?,
    val circuitBreakers: List<CircuitBreakerFact> = emptyList(),   // 기존 저장분 역직렬화 호환을 위해 기본값 유지
    val krTreasury: KrTreasuryFact? = null,
) : MarketFactSheet

/** 한국 국채 만기별 금리 (국내 장 마감 브리핑). 값은 16:05 시점 네이버 시세, 결측 만기는 ECOS 종가 */
data class KrTreasuryFact(
    val yields: List<YieldFact>,
)

data class YieldFact(
    val name: String,
    val yield: Double,
    val changeBp: Double?,
    val fallbackDate: LocalDate? = null,   // 네이버 결측 → ECOS 종가로 대체한 경우 그 기준일 (기존 저장분 호환을 위해 기본값)
    val staleAt: LocalDateTime? = null,    // 네이버 값이 10분 넘게 갱신되지 않았으면 마지막 갱신 시각 ("몇 시 기준" 표기)
)

data class KrPrevFact(
    val tradeDate: LocalDate,
    val kospi: IndexFact?,
    val kospi200: IndexFact?,
    val kosdaq: IndexFact?,
    val kosdaq150: IndexFact?,
)

data class RecommendFact(val grade: String, val names: List<String>)

data class KrPreFactSheet(
    val tradeDate: LocalDate,
    val asOf: LocalDateTime,
    val krHoliday: Boolean,
    val krPrev: KrPrevFact?,
    val recommend: List<RecommendFact>?,
    val macro: MacroFact?,
) : MarketFactSheet

data class UsCloseFactSheet(
    val tradeDate: LocalDate,
    val asOf: LocalDateTime,
    val earlyClose: Boolean,
    val indexes: List<IndexFact>?,
    val treasury: TreasuryFact?,
    val usdKrw: QuoteFact?,
) : MarketFactSheet
