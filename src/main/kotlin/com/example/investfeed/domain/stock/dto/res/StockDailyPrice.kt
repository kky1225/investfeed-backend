package com.example.investfeed.domain.stock.dto.res

data class StockDailyPrice(
    var dt: String? = null, // 일자
    var curPrc: String? = null, // 종가
    var predPreSig: String? = null, // 전일대비기호 2:상승, 3:보합, 5:하락
    var predPre: String? = null, // 전일대비
    var fluRt: String? = null, // 등락률 (%)
    var accTrdeQty: String? = null, // 거래량
)
