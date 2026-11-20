package com.example.investfeed.global.holiday

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface MarketSessionOverrideRepository : JpaRepository<MarketSessionOverride, Long> {
    fun findAllByMarketAndTradeDateBetweenOrderByTradeDate(market: String, from: LocalDate, to: LocalDate): List<MarketSessionOverride>
    fun existsByMarketAndTradeDate(market: String, tradeDate: LocalDate): Boolean
}
