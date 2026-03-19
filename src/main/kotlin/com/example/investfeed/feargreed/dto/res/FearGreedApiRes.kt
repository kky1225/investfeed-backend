package com.example.investfeed.feargreed.dto.res

data class FearGreedApiRes(
    val name: String? = null,
    val data: List<FearGreedData>? = null,
)

data class FearGreedData(
    val value: String? = null,
    val value_classification: String? = null,
    val timestamp: String? = null,
)
