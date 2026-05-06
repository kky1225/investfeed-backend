package com.example.investfeed.domain.auth.dto.req

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class CreateRoleReq(
    @field:NotBlank(message = "권한 코드를 입력해주세요.")
    @field:Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$", message = "권한 코드는 대문자/숫자/언더스코어로 2~50자.")
    val code: String,
    @field:NotBlank(message = "권한 이름을 입력해주세요.")
    val name: String,
    val defaultLandingPath: String? = null,
    val afterRoleId: Long? = null,
)
