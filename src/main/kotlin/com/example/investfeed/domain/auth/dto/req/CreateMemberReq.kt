package com.example.investfeed.domain.auth.dto.req

data class CreateMemberReq(
    val loginId: String,
    val email: String,
    val nickname: String,
    val name: String,
    val phone: String,
    val role: String = "GUEST"
)
