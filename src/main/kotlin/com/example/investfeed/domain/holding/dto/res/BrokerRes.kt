package com.example.investfeed.domain.holding.dto.res

import com.example.investfeed.domain.holding.entity.BrokerType
import com.example.investfeed.domain.holding.entity.MarketType

data class BrokerListRes(
    val brokers: List<BrokerItem>
)

data class BrokerItem(
    val id: Long,
    val name: String,
    val type: BrokerType,
    val market: MarketType
)

data class MyBrokerListRes(
    val brokers: List<MyBrokerItem>
)

data class MyBrokerItem(
    val id: Long,
    val brokerId: Long,
    val name: String,
    val type: BrokerType,
    val market: MarketType,
    val orderIndex: Int
)
