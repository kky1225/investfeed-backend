package com.example.investfeed.domain.menu.dto.req

data class UpdateMenuStructureReq(
    val structures: List<MenuStructureItem>
)

data class MenuStructureItem(
    val id: Long,
    val parentId: Long? = null,
    val orderIndex: Int
)
