package com.example.investfeed.domain.auth.dto.req

data class SignupReq(
    val email: String,
    val password: String,
    val nickname: String,
    val name: String,
    val phone: String
)
