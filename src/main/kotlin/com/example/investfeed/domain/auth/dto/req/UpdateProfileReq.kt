package com.example.investfeed.domain.auth.dto.req

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class UpdateProfileReq(
    @field:NotBlank(message = "닉네임을 입력해주세요.")
    val nickname: String,
    @field:NotBlank(message = "이메일을 입력해주세요.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String,
    @field:NotBlank(message = "이름을 입력해주세요.")
    val name: String,
    @field:NotBlank(message = "전화번호를 입력해주세요.")
    @field:Pattern(regexp = "^[0-9-]{9,13}$", message = "올바른 전화번호 형식이 아닙니다.")
    val phone: String
)
