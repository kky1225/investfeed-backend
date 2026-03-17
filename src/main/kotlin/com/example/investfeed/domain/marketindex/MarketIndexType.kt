package com.example.investfeed.domain.marketindex

enum class MarketIndexType(
    val displayName: String,
) {
    NASDAQ(displayName = "나스닥 종합"),
    SP500(displayName = "S&P 500"),
    VIX(displayName = "VIX"),
    PHILADELPHIA_SEMICONDUCTOR(displayName = "필라델피아 반도체"),

    USD_KRW(displayName = "미국 USD"),
    DOLLAR_INDEX(displayName = "달러인덱스"),

    GOLD_INTERNATIONAL(displayName = "국제 금"),
    WTI(displayName = "WTI"),

    KOSPI(displayName = "코스피"),
    KOSDAQ(displayName = "코스닥"),
}
