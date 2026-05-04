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

data class ApiPatternRes(
    val id: Long,
    val apiPattern: String,
)

data class PermissionActionRes(
    val action: String,
    val description: String?,
)

data class RolePermissionRes(
    val roleId: Long,
    val roleCode: String,
    val roleName: String,
    val actions: List<String>,
)
