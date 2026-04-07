package com.example.investfeed.domain.notification.dto.req

import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.PriceTargetDirection

data class PriceTargetCreateReq(
    val assetType: AssetType,
    val assetCode: String,
    val assetName: String,
    val targetPrice: Long,
    val direction: PriceTargetDirection,
)
