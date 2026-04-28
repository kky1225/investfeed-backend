package com.example.investfeed.domain.menu.dto.res

data class MenuRes(
    val id: Long,
    val name: String,
    val url: String?,
    val icon: String?,
    val parentId: Long?,
    val orderIndex: Int,
    val visible: Boolean,
    val permissions: List<MenuPermissionRes>,
    val requiredBrokerIds: List<Long>,
    val children: List<MenuRes>
)

data class MenuPermissionRes(
    val role: String,
    val readable: Boolean
)
