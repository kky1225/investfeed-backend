package com.example.investfeed.toss.holding.dto.res

data class TossBuyingPowerRes(
    var result: TossBuyingPower? = null
)

data class TossBuyingPower(
    var currency: String? = null,
    var cashBuyingPower: String? = null
)
