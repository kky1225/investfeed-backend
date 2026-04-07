package com.example.investfeed.domain.notification.dto.res

import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.PriceTarget
import com.example.investfeed.domain.notification.entity.PriceTargetDirection
import java.time.LocalDateTime

data class PriceTargetRes(
    val id: Long,
    val assetType: AssetType,
    val assetCode: String,
    val assetName: String,
    val targetPrice: Long,
    val direction: PriceTargetDirection,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(entity: PriceTarget) = PriceTargetRes(
            id = entity.id,
            assetType = entity.assetType,
            assetCode = entity.assetCode,
            assetName = entity.assetName,
            targetPrice = entity.targetPrice,
            direction = entity.direction,
            createdAt = entity.createdAt,
        )
    }
}
