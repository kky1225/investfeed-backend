package com.example.investfeed.domain.notification.dto.req

import com.example.investfeed.domain.notification.entity.AssetType

data class NotificationListReq(
    val assetType: AssetType? = null
)
