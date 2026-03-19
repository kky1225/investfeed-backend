package com.example.investfeed.domain.crypto.dto.res

data class FearGreedRes(
    var current: FearGreedItem,
    var history: List<FearGreedItem>,
)

data class FearGreedItem(
    var value: Int = 0,
    var classification: String = "",
    var date: String = "",
)
