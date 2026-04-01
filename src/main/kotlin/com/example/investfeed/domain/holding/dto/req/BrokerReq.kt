package com.example.investfeed.domain.holding.dto.req

import com.example.investfeed.domain.holding.entity.BrokerType
import com.example.investfeed.domain.holding.entity.MarketType

data class BrokerCreateReq(
    val name: String,
    val type: BrokerType,
    val market: MarketType
)

data class BrokerUpdateReq(
    val name: String,
    val type: BrokerType,
    val market: MarketType
)

data class MyBrokerAddReq(
    val brokerId: Long
)
