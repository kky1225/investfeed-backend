package com.example.investfeed.domain.holding.dto.res

import com.example.investfeed.domain.holding.entity.BrokerType
import com.example.investfeed.domain.holding.entity.MarketType

data class MyBrokerItem(
    val id: Long,
    val brokerId: Long,
    val name: String,
    val type: BrokerType,
    val market: MarketType,
    val orderIndex: Int
)