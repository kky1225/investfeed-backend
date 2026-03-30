package com.example.investfeed.domain.auth.dto.req

data class SecondaryPasswordSetupReq(
    val password: String
)

data class SecondaryPasswordVerifyReq(
    val password: String
)

data class SecondaryPasswordChangeReq(
    val currentPassword: String,
    val newPassword: String
)
