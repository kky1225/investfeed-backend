package com.example.investfeed.domain.auth.dto.req

data class ChangePasswordReq(
    val currentPassword: String,
    val newPassword: String
)
