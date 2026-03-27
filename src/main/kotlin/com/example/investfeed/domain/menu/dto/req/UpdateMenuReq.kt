package com.example.investfeed.domain.menu.dto.req

data class UpdateMenuReq(
    val name: String,
    val url: String? = null,
    val icon: String? = null,
    val parentId: Long? = null,
    val visible: Boolean = true
)
