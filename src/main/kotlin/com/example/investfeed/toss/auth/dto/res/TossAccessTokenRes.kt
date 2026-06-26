package com.example.investfeed.toss.auth.dto.res

data class TossAccessTokenRes(
    var access_token: String? = null, // 접근토큰
    var token_type: String? = null,   // 토큰 타입 (Bearer)
    var expires_in: Long? = null      // 만료까지 남은 시간(초)
)
