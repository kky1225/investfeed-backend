package com.example.investfeed.domain.us.stock.dto.res

/** 차트의 과거 구간 한 페이지. 과거→최신 순 */
data class UsStockCandleRes(
    val candles: List<UsStockChart>,
    val nextCursor: String?,    // 이 값으로 다시 조회하면 그 이전 구간을 받는다. null 이면 더 받을 과거가 없다
)
