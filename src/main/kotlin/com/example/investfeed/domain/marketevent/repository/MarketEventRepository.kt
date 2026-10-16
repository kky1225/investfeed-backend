package com.example.investfeed.domain.marketevent.repository

import com.example.investfeed.domain.marketevent.entity.MarketEvent
import com.example.investfeed.domain.marketevent.entity.MarketEventType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface MarketEventRepository : JpaRepository<MarketEvent, Long> {
    fun findByTradeDateAndEventTypeOrderByStartedAt(tradeDate: LocalDate, eventType: MarketEventType): List<MarketEvent>
}
