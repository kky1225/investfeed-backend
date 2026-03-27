package com.example.investfeed.domain.menu.dto.req

data class UpdateMenuPermissionReq(
    val permissions: List<MenuPermissionItem>
)

data class MenuPermissionItem(
    val role: String,
    val readable: Boolean
)
