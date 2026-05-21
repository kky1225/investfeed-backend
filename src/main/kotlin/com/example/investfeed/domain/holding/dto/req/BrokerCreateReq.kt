package com.example.investfeed.domain.holding.dto.req

import com.example.investfeed.domain.holding.entity.BrokerType
import com.example.investfeed.domain.holding.entity.MarketType
import jakarta.validation.constraints.NotBlank

data class BrokerCreateReq(
    @field:NotBlank(message = "증권사명을 입력해주세요.")
    val name: String,
    val type: BrokerType,
    val market: MarketType
)