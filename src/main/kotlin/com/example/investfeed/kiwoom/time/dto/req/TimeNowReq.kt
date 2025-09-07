package com.example.investfeed.kiwoom.time.dto.req

import com.example.investfeed.kiwoom.config.MarketType

data class TimeNowReq(
    var marketType: MarketType
)