package com.example.investfeed.domain.notification.entity

enum class Direction {
    UP,
    DOWN,
    UPPER_LIMIT,
    LOWER_LIMIT,
    HIGH_52W,
    LOW_52W,
    TARGET_ABOVE,
    TARGET_BELOW,
    GOAL_ACHIEVED,
    REBALANCING_ASSET,
    REBALANCING_STOCK,
    API_KEY_EXPIRY_30D,
    API_KEY_EXPIRY_7D,
    API_KEY_EXPIRED
}
