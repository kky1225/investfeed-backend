package com.example.investfeed.domain.auth.dto.res

data class RoleRes(
    val id: Long,
    val code: String,
    val name: String,
    val defaultLandingPath: String?,
    val isSystem: Boolean,
    val priority: Int,
    val orderIndex: Int,
)
