package com.example.investfeed.kiwoom.config

enum class ResponseCode(
    val code: String = "0000",
    val message: String
) {
    ACCESS_TOKEN(message = "접근토큰발급 성공"),

    SECT_PRICE(message = "업종별 주가 조회 성공"),
    SECT_INVESTOR(message = "업종별투자자순매수 조회 성공")
}