package com.example.investfeed.domain.us.exchange.dto.res

import com.example.investfeed.domain.marketindex.dto.res.MarketIndexRes

data class UsExchangeRateRes(
    val krwToUsd: UsExchangeRateItem?, // 원화 -> 달러 환전 적용환율
    val usdToKrw: UsExchangeRateItem?, // 달러 -> 원화 환전 적용환율
    val marketIndex: MarketIndexRes?, // 시장 USD/KRW 시세 (Redis 미적재 시 null)
)
