package com.example.investfeed.domain.assistant.dto.req

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class PriceAlertDeleteActionReq(
    @field:NotBlank val cardRef: String,
    @field:NotNull val targetId: Long?,   // 카드에서 고른 알림 (확인 카드에 담긴 알림만 삭제)
)
