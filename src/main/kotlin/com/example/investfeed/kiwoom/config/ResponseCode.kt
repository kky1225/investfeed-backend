package com.example.investfeed.kiwoom.config

enum class ResponseCode(
    val code: String,
    val message: String
) {
    ACCESS_TOKEN("0000", "접근토큰발급 성공"),

    SECT_PRICE("0000", "업종별 주가 조회 성공"),
    SECT_INVESTOR("0000", "업종별투자자순매수 조회 성공")
}