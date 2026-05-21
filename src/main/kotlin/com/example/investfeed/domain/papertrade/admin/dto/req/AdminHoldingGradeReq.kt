package com.example.investfeed.domain.papertrade.admin.dto.req

import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

/** 보유 평가 조회 요청. evalDate 미지정 시 service 에서 가장 최근 평가일 사용. */
data class AdminHoldingGradeReq(
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val evalDate: LocalDate? = null,
)
