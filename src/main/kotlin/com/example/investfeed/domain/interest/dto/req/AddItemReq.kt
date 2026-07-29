package com.example.investfeed.domain.interest.dto.req

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class AddItemReq(
    @field:NotBlank(message = "종목 코드를 확인해주세요.")
    val stkCd: String,
    @field:NotBlank(message = "종목명을 확인해주세요.")
    val stkNm: String,
    @field:Pattern(regexp = "ND|NY|NA", message = "거래소 구분을 확인해주세요.")
    val stexTp: String? = null // NULL: 국내, ND/NY/NA: 미국 거래소
)
