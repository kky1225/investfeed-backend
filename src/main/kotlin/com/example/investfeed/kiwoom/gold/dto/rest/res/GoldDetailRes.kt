package com.example.investfeed.kiwoom.gold.dto.rest.res

data class GoldDetailRes<T>(
    var goldPriceNowRes: GoldPriceNowRes? = null,
    var chartListRes: T? = null,
    var goldInvestor: GoldInvestorRes? = null
)