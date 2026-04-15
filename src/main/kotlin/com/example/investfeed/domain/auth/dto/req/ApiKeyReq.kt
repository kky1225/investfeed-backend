package com.example.investfeed.domain.auth.dto.req

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class ApiKeyReq(
    @field:Positive(message = "제공자를 선택해주세요.")
    val brokerId: Long,
    @field:NotBlank(message = "App Key를 입력해주세요.")
    val appKey: String,
    @field:NotBlank(message = "Secret Key를 입력해주세요.")
    val secretKey: String
)
