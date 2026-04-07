package com.example.investfeed.upbit.holding.dto.res

data class UpbitAccountRes(
    val currency: String? = null,
    val balance: String? = null,
    val locked: String? = null,
    val avg_buy_price: String? = null,
    val avg_buy_price_modified: Boolean? = null,
    val unit_currency: String? = null,
)
