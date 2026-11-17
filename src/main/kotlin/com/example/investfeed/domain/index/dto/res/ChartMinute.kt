package com.example.investfeed.domain.index.dto.res

/** 목록 화면 카드의 당일 분봉 한 개. 시가·고가·저가는 카드를 캔들로 그릴 때 쓴다 */
data class ChartMinute(
    var curPrc: String? = null,
    var cntrTm: String? = null,
    var openPric: String? = null,
    var highPric: String? = null,
    var lowPric: String? = null,
)
