package com.example.investfeed.domain.permission.dto.res

data class PermissionRes(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val isSystem: Boolean,
    val orderIndex: Int,
    val apiPatterns: List<ApiPatternRes>,
    val supportedActions: List<PermissionActionRes>,
    val rolePermissions: List<RolePermissionRes>,
)
