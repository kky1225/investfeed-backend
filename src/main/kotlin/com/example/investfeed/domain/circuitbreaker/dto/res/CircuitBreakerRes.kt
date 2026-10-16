package com.example.investfeed.domain.circuitbreaker.dto.res

import java.time.LocalDateTime

data class CircuitBreakerRes(
    val indexCode: String,        // KOSPI, KOSDAQ
    val stage: Int,               // 1~3
    val triggerRate: Double,
    val firedAt: LocalDateTime,
    val resumeAt: LocalDateTime?, // 3단계는 null (당일 장 종료)
    val halted: Boolean,          // 현재 매매 정지 중 여부
)
