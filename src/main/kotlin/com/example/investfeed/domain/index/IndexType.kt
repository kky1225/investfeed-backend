package com.example.investfeed.domain.index

enum class IndexType(
    val indsCd: String,
    val indsNm: String,
    val marketType: String,
) {
    KOSPI(indsCd = "001", indsNm = "종합(KOSPI)", marketType = "0"),
    KOSDAQ(indsCd = "101", indsNm = "종합(KOSDAQ)", marketType = "1"),
    KOSPI200(indsCd = "201", indsNm = "KOSPI 200", marketType = ""),
}