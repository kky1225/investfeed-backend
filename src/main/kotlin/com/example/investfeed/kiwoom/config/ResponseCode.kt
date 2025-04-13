package com.example.investfeed.kiwoom.config

enum class ResponseCode(
    val code: String,
    val message: String
) {
    ACCESS_TOKEN("0000", "접근토큰발급 성공")
}