package com.example.investfeed.domain.permission.dto.req

import jakarta.validation.constraints.NotBlank

data class UpdatePermissionReq(
    @field:NotBlank(message = "권한 이름을 입력해주세요.")
    val name: String,
    val description: String? = null,
)
