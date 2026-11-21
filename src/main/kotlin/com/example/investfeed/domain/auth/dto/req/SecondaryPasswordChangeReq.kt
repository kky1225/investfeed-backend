package com.example.investfeed.domain.auth.dto.req

import jakarta.validation.constraints.Pattern

data class SecondaryPasswordChangeReq(
    // 현재 값은 형식을 보지 않는다. 형식이 달라도 대조에서 틀린 것으로 세어져야 한다.
    val currentPassword: String,
    @field:Pattern(regexp = SECONDARY_PASSWORD_PATTERN, message = SECONDARY_PASSWORD_PATTERN_MESSAGE)
    val newPassword: String
)
