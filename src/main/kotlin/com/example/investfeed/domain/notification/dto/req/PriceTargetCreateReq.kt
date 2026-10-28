package com.example.investfeed.domain.notification.dto.req

import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.PriceTargetDirection
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class PriceTargetCreateReq(
    val assetType: AssetType,
    @field:NotBlank(message = "종목을 확인해주세요.")
    val assetCode: String,
    @field:NotBlank(message = "종목명을 확인해주세요.")
    val assetName: String,
    @field:Positive(message = "목표가를 입력해주세요.")
    val targetPrice: BigDecimal,   // 국내·코인 원, 미국(_US) 달러
    val direction: PriceTargetDirection,
)
