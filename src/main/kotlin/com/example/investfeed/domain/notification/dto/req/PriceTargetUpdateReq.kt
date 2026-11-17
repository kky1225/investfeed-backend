package com.example.investfeed.domain.notification.dto.req

import com.example.investfeed.domain.notification.entity.PriceTargetDirection
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

/** 목표가 알림의 가격을 바꾼다. 현재가의 반대편으로 옮기면 방향도 달라지므로 함께 받는다. */
data class PriceTargetUpdateReq(
    @field:Positive(message = "목표가를 입력해주세요.")
    val targetPrice: BigDecimal,   // 국내·코인 원, 미국(_US) 달러
    val direction: PriceTargetDirection,
)
