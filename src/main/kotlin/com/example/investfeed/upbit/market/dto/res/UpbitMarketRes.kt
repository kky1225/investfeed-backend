package com.example.investfeed.upbit.market.dto.res

data class UpbitMarketRes(
    val market: String,
    val korean_name: String,
    val english_name: String,
    val market_event: MarketEvent? = null,
)

data class MarketEvent(
    val warning: Boolean? = null,
    val caution: Caution? = null,
)

data class Caution(
    val PRICE_FLUCTUATIONS: Boolean? = null,
    val TRADING_VOLUME_SOARING: Boolean? = null,
    val DEPOSIT_AMOUNT_SOARING: Boolean? = null,
    val GLOBAL_PRICE_DIFFERENCES: Boolean? = null,
    val CONCENTRATION_OF_SMALL_ACCOUNTS: Boolean? = null,
)
