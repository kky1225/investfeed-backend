package com.example.investfeed.domain.circuitbreaker.dto

import com.example.investfeed.domain.marketevent.entity.MarketEvent

data class CircuitBreakerFiredEvent(
    val event: MarketEvent,
    val prevClose: Double,
    val high: Double,
    val low: Double,
)
