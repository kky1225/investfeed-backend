package com.example.investfeed.kiwoom.config

enum class ResponseCode(
    val code: String = "0000",
    val message: String
) {
    ACCESS_TOKEN(message = "접근토큰발급 성공"),

    SECT_PRICE(message = "업종별 주가 조회에 성공하셨습니다."),
    SECT_INVESTOR(message = "업종별 투자자 순매수 조회에 성공하셨습니다."),
    SECT_PRICE_NOW(message = "업종 현재가 조회에 성공하셨습니다."),
    SECT_CODE_LIST(message = "업종 코드 리스트 조회에 성공하셨습니다."),
    SECT_INDEX_LIST(message = "전업종 지수 조회에 성공하셨습니다.")
}