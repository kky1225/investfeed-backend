package com.example.investfeed.domain.auth.dto.req

data class ApiKeyReq(
    val brokerId: Long,
    val appKey: String,
    val secretKey: String
)
