package com.example.investfeed.domain.time.dto.req

import com.example.investfeed.domain.time.MarketType

data class TimeNowReq(
    var marketType: MarketType
)
