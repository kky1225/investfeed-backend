package com.example.investfeed.domain.calendar.dto.req

import jakarta.validation.constraints.NotBlank

data class ManualCalendarEventReq(
    @field:NotBlank(message = "날짜를 입력해주세요.")
    val date: String,     // yyyy-MM-dd
    @field:NotBlank(message = "일정명을 입력해주세요.")
    val name: String,
    @field:NotBlank(message = "국가를 선택해주세요.")
    val country: String,  // KR / US
    val value: String? = null,
    @field:NotBlank(message = "유형을 선택해주세요.")
    val type: String,     // RATE_DECISION, US_RATE_DECISION, GDP_RELEASE, CUSTOM, HOLIDAY(휴장일)
)