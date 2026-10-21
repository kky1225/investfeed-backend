package com.example.investfeed.internal.assistant.dto.req

import com.example.investfeed.internal.assistant.tool.RankToolService

data class TradingRankReq(
    val market: RankToolService.RankMarket,   // 국내/미국 필수 — 질문에 없으면 비서가 되묻는다
    val type: RankToolService.RankType,
    val date: String? = null,                 // 미지정 = 당일(개장 전·휴장일이면 직전 거래일). 과거 날짜는 안내 문장. 미국은 현지 날짜
)
