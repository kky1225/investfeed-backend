package com.example.investfeed.domain.monitoring.dto.res

data class RedisOverviewRes(
    val redis: RedisCacheRes,
    val unackCount: UnacknowledgedCountRes,
)