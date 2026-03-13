package com.example.investfeed.domain.auth.dto.req

data class SignupReq(
    val loginId: String,
    val password: String,
    val email: String,
    val nickname: String,
    val name: String,
    val phone: String
)
