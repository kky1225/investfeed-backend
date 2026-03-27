package com.example.investfeed.domain.menu.dto.req

data class CreateMenuReq(
    val name: String,
    val url: String? = null,
    val icon: String? = null,
    val parentId: Long? = null,
    val orderIndex: Int = 0,
    val visible: Boolean = true
)
