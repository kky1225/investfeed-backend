package com.example.investfeed.domain.assistant.dto.factsheet

import java.time.LocalDate
import java.time.LocalDateTime

sealed interface MarketFactSheet

data class QuoteFact(
    val price: Double,
    val changeRate: Double? = null,
    val changeAmount: Double? = null,
    val delayStatus: String? = null,
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
    val y10: Double?, val y10Bp: Double?,
    val spreadBp: Int?,
    val source: String,
    val asOfDate: LocalDate,
    val stale: Boolean = false,
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
) : MarketFactSheet

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
