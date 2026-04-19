package com.example.investfeed.domain.notification.dto.res

data class NotificationSettingRes(
    val priceUpEnabled: Boolean,
    val priceDownEnabled: Boolean,
    val high52wEnabled: Boolean,
    val low52wEnabled: Boolean,
    val upperLimitEnabled: Boolean,
    val lowerLimitEnabled: Boolean,
    val goalEnabled: Boolean,
    val rebalancingEnabled: Boolean,
    val apiKeyEnabled: Boolean
)
