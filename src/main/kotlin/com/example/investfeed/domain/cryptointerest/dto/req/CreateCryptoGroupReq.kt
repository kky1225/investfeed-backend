package com.example.investfeed.domain.cryptointerest.dto.req

import jakarta.validation.constraints.NotBlank

data class CreateCryptoGroupReq(
    @field:NotBlank(message = "그룹명을 입력해주세요.")
    val groupNm: String,
)
