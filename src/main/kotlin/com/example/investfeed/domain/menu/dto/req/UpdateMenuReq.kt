package com.example.investfeed.domain.menu.dto.req

import jakarta.validation.constraints.NotBlank

data class UpdateMenuReq(
    @field:NotBlank(message = "메뉴명을 입력해주세요.")
    val name: String,
    val url: String? = null,
    val icon: String? = null,
    val parentId: Long? = null,
    val requiredPermissionId: Long? = null,
    val visible: Boolean = true
)
