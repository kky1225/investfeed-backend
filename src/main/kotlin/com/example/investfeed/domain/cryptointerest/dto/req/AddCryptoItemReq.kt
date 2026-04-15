package com.example.investfeed.domain.cryptointerest.dto.req

import jakarta.validation.constraints.NotBlank

data class AddCryptoItemReq(
    @field:NotBlank(message = "마켓 코드를 확인해주세요.")
    val market: String,
    @field:NotBlank(message = "코인명을 확인해주세요.")
    val koreanName: String,
)
