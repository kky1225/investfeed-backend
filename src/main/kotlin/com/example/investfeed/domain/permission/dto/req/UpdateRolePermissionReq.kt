package com.example.investfeed.domain.permission.dto.req

data class UpdateRolePermissionReq(
    val grants: List<RolePermissionGrant>
)

data class RolePermissionGrant(
    val roleCode: String,
    val actions: List<String>,
)
