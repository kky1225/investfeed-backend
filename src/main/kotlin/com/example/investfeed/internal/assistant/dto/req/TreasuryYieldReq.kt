package com.example.investfeed.internal.assistant.dto.req

import com.example.investfeed.internal.assistant.tool.TreasuryToolService

data class TreasuryYieldReq(
    val market: TreasuryToolService.TreasuryMarket? = null, // 미지정 = 한국·미국 둘 다
    val date: String? = null,                               // 미지정 = 당일(네이버). 지정 = 그날 종가(미국 FRED·한국 ECOS)
)
