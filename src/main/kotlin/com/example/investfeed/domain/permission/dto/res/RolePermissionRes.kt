package com.example.investfeed.domain.permission.dto.res

data class RolePermissionRes(
    val roleId: Long,
    val roleCode: String,
    val roleName: String,
    val actions: List<String>,
)