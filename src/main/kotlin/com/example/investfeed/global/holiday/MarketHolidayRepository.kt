package com.example.investfeed.global.holiday

import org.springframework.data.jpa.repository.JpaRepository

interface MarketHolidayRepository : JpaRepository<MarketHoliday, Long> {
    fun findAllByMarketAndDtBetween(market: String, startDt: String, endDt: String): List<MarketHoliday>
    fun findAllByMarketAndSourceAndDtBetween(market: String, source: String, startDt: String, endDt: String): List<MarketHoliday>
    fun countByMarket(market: String): Long
}
