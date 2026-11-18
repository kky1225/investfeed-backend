package com.example.investfeed.domain.us.stock.dto.req

data class UsStockCandleReq(
    val stexTp: String = "", // 거래소구분 NA:AMEX, ND:NASDAQ, NY:NYSE
    val chartType: UsStockChartType = UsStockChartType.DAY,
    val cursor: String? = null,     // 앞선 응답의 nextCursor. 없으면 가장 최근 구간부터
)
