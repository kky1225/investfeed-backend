package com.example.investfeed.domain.monitoring.dto.res

data class RedisPrefixRes(
    val prefix: String,
    val description: String,
    val keyCount: Long,
    val minTtlSec: Long?,
    val maxTtlSec: Long?,
)

data class RedisCacheRes(
    val prefixes: List<RedisPrefixRes>,
)
