package com.example.investfeed.domain.auth.dto.req

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordReq(
    @field:NotBlank(message = "현재 비밀번호를 입력해주세요.")
    val currentPassword: String,
    @field:NotBlank(message = "새 비밀번호를 입력해주세요.")
    @field:Size(min = 6, message = "새 비밀번호를 6자 이상 입력해주세요.")
    val newPassword: String
)
