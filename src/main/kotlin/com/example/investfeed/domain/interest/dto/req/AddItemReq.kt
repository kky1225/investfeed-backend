package com.example.investfeed.domain.interest.dto.req

import jakarta.validation.constraints.NotBlank

data class AddItemReq(
    @field:NotBlank(message = "종목 코드를 확인해주세요.")
    val stkCd: String,
    @field:NotBlank(message = "종목명을 확인해주세요.")
    val stkNm: String
)
