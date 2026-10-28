package com.example.investfeed.domain.cryptowatchlist.dto.req

import jakarta.validation.constraints.NotBlank

data class UpdateCryptoGroupReq(
    @field:NotBlank(message = "그룹명을 입력해주세요.")
    val groupNm: String,
)
