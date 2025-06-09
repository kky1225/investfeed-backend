package com.example.investfeed.kiwoom.auth.dto.res

data class AccessTokenRes (
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var expires_dt: String? = null, // 만료일
    var token_type: String? = null, // 토큰타입
    var token: String? = null // 접근토큰
)