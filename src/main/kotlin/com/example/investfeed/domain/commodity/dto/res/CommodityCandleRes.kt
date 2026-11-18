package com.example.investfeed.domain.commodity.dto.res

/** 차트의 과거 구간 한 페이지. 최신→과거 순. 분봉은 이어 받을 과거가 없어 늘 비어 있다 */
data class CommodityCandleRes(
    val candles: List<CommodityChart>,
    val nextCursor: String?,    // 이 값으로 다시 조회하면 그 이전 구간을 받는다. null 이면 더 받을 과거가 없다
)
