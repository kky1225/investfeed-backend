package com.example.investfeed.toss.market.dto.res

data class TossCandleRes(
    var result: TossCandlePage? = null
)

data class TossCandlePage(
    var candles: List<TossCandle>? = null, // 최신순
    var nextBefore: String? = null, // 다음 페이지 before 값. 마지막이면 null
)

data class TossCandle(
    var timestamp: String? = null, // 1d: 미국 거래일 현지 자정을 KST 로 표기 (예: 2026-10-19T13:00:00+09:00)
    var openPrice: String? = null,
    var highPrice: String? = null,
    var lowPrice: String? = null,
    var closePrice: String? = null, // 1d: 정규장 공식 종가 (프리·애프터·데이마켓 미포함, 토스 FAQ)
    var volume: String? = null, // 1d: 종합 거래량 (시간외 포함 가능)
    var currency: String? = null,
)

data class TossUsMarketCalendarRes(
    var result: TossUsMarketCalendar? = null
)

data class TossUsMarketCalendar(
    var previousBusinessDay: TossUsMarketDay? = null,
    var today: TossUsMarketDay? = null,
    var nextBusinessDay: TossUsMarketDay? = null,
)

/** 미국 거래일 = 뉴욕 기준 전일 20:00 ~ 당일 20:00. 세션 순서 데이마켓 → 프리 → 정규 → 애프터. 휴장이면 세션 모두 null */
data class TossUsMarketDay(
    var date: String? = null, // 영업일 (미국 현지, yyyy-MM-dd)
    var dayMarket: TossMarketSession? = null,
    var preMarket: TossMarketSession? = null,
    var regularMarket: TossMarketSession? = null,
    var afterMarket: TossMarketSession? = null,
)

data class TossMarketSession(
    var startTime: String? = null, // ISO 8601 (+09:00)
    var endTime: String? = null,
)
