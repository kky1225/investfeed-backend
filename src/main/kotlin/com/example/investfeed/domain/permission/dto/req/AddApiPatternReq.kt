package com.example.investfeed.domain.permission.dto.req

import jakarta.validation.constraints.NotBlank

data class AddApiPatternReq(
    @field:NotBlank(message = "API 패턴을 입력해주세요.")
    val apiPattern: String,
)
