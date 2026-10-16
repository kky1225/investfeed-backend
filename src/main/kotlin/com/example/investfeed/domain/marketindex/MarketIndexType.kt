package com.example.investfeed.domain.marketindex

enum class MarketIndexType(
    val displayName: String,
) {
    NASDAQ(displayName = "나스닥 종합"),
    SP500(displayName = "S&P 500"),
    VIX(displayName = "VIX"),
    PHILADELPHIA_SEMICONDUCTOR(displayName = "필라델피아 반도체"),
    DOW(displayName = "다우존스"),

    USD_KRW(displayName = "미국 USD"),
    DOLLAR_INDEX(displayName = "달러인덱스"),

    GOLD_INTERNATIONAL(displayName = "국제 금"),
    WTI(displayName = "WTI"),

    US_TREASURY_2Y(displayName = "미국 국채 2년"),
    US_TREASURY_5Y(displayName = "미국 국채 5년"),
    US_TREASURY_10Y(displayName = "미국 국채 10년"),
    US_TREASURY_30Y(displayName = "미국 국채 30년"),

    KR_TREASURY_2Y(displayName = "한국 국채 2년"),
    KR_TREASURY_3Y(displayName = "한국 국채 3년"),
    KR_TREASURY_5Y(displayName = "한국 국채 5년"),
    KR_TREASURY_10Y(displayName = "한국 국채 10년"),
    KR_TREASURY_20Y(displayName = "한국 국채 20년"),
    KR_TREASURY_30Y(displayName = "한국 국채 30년"),

    KOSPI(displayName = "코스피"),
    KOSDAQ(displayName = "코스닥"),
}
