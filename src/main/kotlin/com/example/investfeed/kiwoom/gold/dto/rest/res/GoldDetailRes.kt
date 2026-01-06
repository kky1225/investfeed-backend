package com.example.investfeed.kiwoom.gold.dto.rest.res

import com.example.investfeed.kiwoom.investor.dto.res.KiwoomGoldInvestorRes
import com.example.investfeed.kiwoom.price.dto.res.KiwoomGoldPriceNowRes

data class GoldDetailRes<T>(
    var kiwoomGoldPriceNowRes: KiwoomGoldPriceNowRes? = null,
    var goldPriceNowMinuteRes: GoldPriceNowMinuteRes? = null,
    var chartListRes: T? = null,
    var goldInvestor: KiwoomGoldInvestorRes? = null
)