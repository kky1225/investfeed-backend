package com.example.investfeed.domain.auth.dto.res

data class TokenRes(
    val accessToken: String,
    val passwordChangeRequired: Boolean = false,
    val role: String? = null,
    val nickname: String? = null,
    val email: String? = null
)
