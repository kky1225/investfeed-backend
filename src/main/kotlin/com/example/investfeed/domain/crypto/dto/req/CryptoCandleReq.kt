package com.example.investfeed.domain.crypto.dto.req

data class CryptoCandleReq(
    val chartType: CryptoChartType = CryptoChartType.DAY,
    val cursor: String? = null,     // 앞선 응답의 nextCursor. 없으면 가장 최근 구간부터
)
