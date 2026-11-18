package com.example.investfeed.domain.index.dto.res

/** 차트의 과거 구간 한 페이지. 최신→과거 순. 가격은 지수 상세의 차트와 같이 100배로 온 값 그대로다 */
data class IndexCandleRes(
    val candles: List<IndexChart>,
    val nextCursor: String?,    // 이 값으로 다시 조회하면 그 이전 구간을 받는다. null 이면 더 받을 과거가 없다
)
