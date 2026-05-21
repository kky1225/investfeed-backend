package com.example.investfeed.domain.papertrade.admin.dto.req

import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

/** 모의계좌 거래내역 조회 요청. ordDt 미지정 시 service 에서 오늘로 처리. */
data class AdminPaperTradeHistoryReq(
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val ordDt: LocalDate? = null,
)