package com.example.investfeed.domain.calendar.dto.req

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

/** 국내 장 시각이 평소와 다른 날 등록·수정 (수능일 등) */
data class MarketSessionReq(
    @field:NotBlank(message = "날짜를 입력해주세요.")
    @field:Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$", message = "날짜를 yyyy-MM-dd 형식으로 입력해주세요.")
    val date: String,        // yyyy-MM-dd
    @field:NotBlank(message = "이름을 입력해주세요.")
    val name: String,
    @field:Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "개장 시각을 HH:mm 형식으로 입력해주세요.")
    val openTime: String,    // HH:mm
    @field:Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "마감 시각을 HH:mm 형식으로 입력해주세요.")
    val closeTime: String,   // HH:mm
    @field:NotNull(message = "프리마켓 운영 여부를 선택해주세요.")
    val preMarket: Boolean?, // NXT 프리마켓(08:00~08:50) 운영 여부
)
