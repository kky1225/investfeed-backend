package com.example.investfeed.domain.auth.dto.req

data class UpdateProfileReq(
    val nickname: String,
    val email: String,
    val name: String,
    val phone: String
)
