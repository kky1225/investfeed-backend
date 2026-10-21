package com.example.investfeed.internal.assistant.dto.req

import com.example.investfeed.internal.assistant.tool.CircuitBreakerToolService

data class CircuitBreakerReq(
    val market: CircuitBreakerToolService.CbMarket,
    val index: CircuitBreakerToolService.CbIndex? = null, // 지수를 직접 물었을 때 (국내는 그 지수만, 미국은 안내 문구만)
    val date: String? = null,                             // 미지정 = 오늘(휴장일이면 직전 거래일)
)
