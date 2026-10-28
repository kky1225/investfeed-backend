package com.example.investfeed.domain.watchlist.dto.req

import jakarta.validation.constraints.NotBlank

data class CreateGroupReq(
    @field:NotBlank(message = "그룹명을 입력해주세요.")
    val groupNm: String
)
