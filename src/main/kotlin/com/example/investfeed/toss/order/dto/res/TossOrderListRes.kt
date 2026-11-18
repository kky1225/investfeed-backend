package com.example.investfeed.toss.order.dto.res

/** 주문 목록 응답 (GET /api/v1/orders). 쓰는 필드만 받는다. */
data class TossOrderListRes(
    var result: TossOrderPage? = null
)

data class TossOrderPage(
    var orders: List<TossOrder>? = null,
    var hasNext: Boolean? = null,
    var nextCursor: String? = null,     // 다음 쪽을 받을 때 그대로 넘기는 값
)

/** 주문 한 건. 종료된 주문(CLOSED)에는 취소·거부된 주문도 섞여 온다(체결 수량 0, filledAt 없음). */
data class TossOrder(
    var symbol: String? = null,         // 국내 6자리 / 미국 티커
    var side: String? = null,           // BUY / SELL
    var currency: String? = null,       // KRW / USD
    var execution: TossOrderExecution? = null,
)

data class TossOrderExecution(
    var filledQuantity: String? = null,         // 체결 수량 (소수점 주문이면 소수)
    var averageFilledPrice: String? = null,     // 평균 체결가 (종목 통화 기준)
    var filledAt: String? = null,               // 최종 체결 시각 (2026-11-17T23:31:05.123+09:00)
)
