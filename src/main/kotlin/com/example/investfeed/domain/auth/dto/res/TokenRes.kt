package com.example.investfeed.domain.auth.dto.res

data class TokenRes(
    val passwordChangeRequired: Boolean = false,
    val role: String? = null,
    val nickname: String? = null,
    val email: String? = null,
    val secondaryPasswordEnabled: Boolean = false,
    val defaultPath: String? = null,
    val permissions: List<UserPermissionRes> = emptyList()
)
