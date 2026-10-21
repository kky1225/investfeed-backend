package com.example.investfeed.internal.assistant.dto.req

import com.example.investfeed.internal.assistant.tool.DataToolService

data class MarketInvestorFlowReq(
    val investor: DataToolService.Investor,
    val days: Int = 1,
    val side: DataToolService.Side,   // 순매수/순매도 필수 — 질문에 없으면 비서가 되묻는다
    val minStreakDays: Int? = null,
    val date: String? = null,         // 미지정 = 최근 기준. 지정하면 그날까지 days 거래일
)
