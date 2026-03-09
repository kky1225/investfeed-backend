package com.example.investfeed.domain.index.dto.res

data class ProgramChart(
    var cntrTm: String? = null, // 체결시간
    var dfrtTrdeNetprps: String? = null, // 차익거래순매수
    var ndiffproTrdeNetprps: String? = null, // 비차익거래순매수
    var allNetprps: String? = null, // 전체순매수
)