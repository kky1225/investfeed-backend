package com.example.investfeed.domain.auth.dto.res

data class UserPermissionRes(
    val code: String,
    val actions: List<String>
)
