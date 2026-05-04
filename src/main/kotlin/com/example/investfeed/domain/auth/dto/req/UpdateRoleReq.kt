package com.example.investfeed.domain.auth.dto.req

import jakarta.validation.constraints.NotBlank

data class UpdateRoleReq(
    @field:NotBlank(message = "권한 이름을 입력해주세요.")
    val name: String,
    val defaultLandingPath: String? = null,
)
