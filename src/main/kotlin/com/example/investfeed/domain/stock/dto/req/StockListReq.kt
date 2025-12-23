package com.example.investfeed.domain.stock.dto.req

data class StockListReq(
    var type: String, // 0: 거래대금, 1: 거래량, 2: 급등
)