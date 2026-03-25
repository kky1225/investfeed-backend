package com.example.investfeed.domain.auth.dto.req

data class ApiKeyReq(
    val provider: String,
    val appKey: String,
    val secretKey: String
)
