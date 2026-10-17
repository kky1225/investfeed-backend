package com.example.investfeed.domain.index.dto.res

import com.example.investfeed.domain.circuitbreaker.dto.res.CircuitBreakerRes

data class IndexDetailRes(
    var indexInfo: IndexInfo? = null,
    var chartList: List<IndexChart>? = null,
    var programChartList: List<ProgramChart>? = null,
    var programList: List<ProgramListItem>? = null,
    var investorDailyList: List<IndexInvestorDailyItem>? = null,
    var circuitBreakers: List<CircuitBreakerRes> = emptyList(),   // 당일 서킷브레이커 발동 (코스피 001·코스닥 101 만)
)