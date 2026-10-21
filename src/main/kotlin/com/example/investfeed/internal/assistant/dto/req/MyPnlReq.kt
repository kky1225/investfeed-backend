package com.example.investfeed.internal.assistant.dto.req

import com.example.investfeed.internal.assistant.tool.DisplayToolService
import com.example.investfeed.internal.assistant.tool.PeriodResolver

data class MyPnlReq(
    val period: PeriodResolver.Period = PeriodResolver.Period.THIS_MONTH,
    val assetClass: DisplayToolService.AssetClass? = null,
)
