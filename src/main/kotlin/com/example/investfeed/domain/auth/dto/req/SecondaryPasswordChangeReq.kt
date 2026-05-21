package com.example.investfeed.domain.auth.dto.req

data class SecondaryPasswordChangeReq(
    val currentPassword: String,
    val newPassword: String
)