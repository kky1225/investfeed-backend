package com.example.investfeed.domain.holding.repository

import com.example.investfeed.domain.holding.entity.Broker
import com.example.investfeed.domain.holding.entity.MarketType
import org.springframework.data.jpa.repository.JpaRepository

interface BrokerRepository : JpaRepository<Broker, Long> {
    fun findByName(name: String): Broker?
    fun findAllByOrderByIdAsc(): List<Broker>
    fun findAllByMarketOrderByIdAsc(market: MarketType): List<Broker>
}
