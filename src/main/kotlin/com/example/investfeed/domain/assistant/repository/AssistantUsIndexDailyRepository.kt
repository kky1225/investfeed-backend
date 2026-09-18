package com.example.investfeed.domain.assistant.repository

import com.example.investfeed.domain.assistant.entity.AssistantUsIndexDaily
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface AssistantUsIndexDailyRepository : JpaRepository<AssistantUsIndexDaily, Long> {
    fun findBySeriesAndTradeDate(series: String, tradeDate: LocalDate): AssistantUsIndexDaily?
    fun findTop30BySeriesOrderByTradeDateDesc(series: String): List<AssistantUsIndexDaily>
}
