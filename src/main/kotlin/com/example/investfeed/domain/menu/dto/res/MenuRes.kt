package com.example.investfeed.domain.menu.dto.res

data class MenuRes(
    val id: Long,
    val name: String,
    val url: String?,
    val icon: String?,
    val parentId: Long?,
    val requiredPermissionId: Long?,
    val requiredPermissionCode: String?,
    val requiredPermissionName: String?,
    val orderIndex: Int,
    val visible: Boolean,
    val requiredBrokerIds: List<Long>,
    val children: List<MenuRes>
)
