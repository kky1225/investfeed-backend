package com.example.investfeed.domain.stock.dto.res

data class StockShortSelling(
    var dt: String? = null, // 일자
    var trdeQty: String? = null, // 거래량
    var shrtsQty: String? = null, // 공매도량
    var trdeWght: String? = null, // 매매비중
    var shrtsTrdePrica: String? = null, // 공매도거래대금
    var shrtsAvgPric: String? = null, // 공매도평균가
)