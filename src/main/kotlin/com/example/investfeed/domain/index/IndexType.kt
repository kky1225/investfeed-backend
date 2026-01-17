package com.example.investfeed.domain.index

enum class IndexType(
    val indsCd: String,
    val indsNm: String
) {
    KOSPI(indsCd = "001", indsNm = "종합(KOSPI)"),
    KOSDAQ(indsCd = "101", indsNm = "종합(KOSDAQ)"),
    KOSPI200(indsCd = "201", indsNm = "KOSPI 200"),
    KOSDAQ150(indsCd = "150", indsNm = "KOSDAQ 150"),
}