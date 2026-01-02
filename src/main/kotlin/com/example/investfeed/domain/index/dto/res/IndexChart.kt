package com.example.investfeed.domain.index.dto.res

data class IndexChart(
    var curPrc: String? = null, // 현재가
    var trdeQty: String? = null, // 거래량
    var dt: String? = null, // 일자
    var openPric: String? = null, // 시가
    var highPric: String? = null, // 고가
    var lowPric: String? = null, // 저가
    var trdePrica: String? = null, // 거래대금
)