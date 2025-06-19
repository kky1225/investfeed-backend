package com.example.investfeed.kiwoom.dashboard.dto.res

import com.example.investfeed.kiwoom.investor.dto.res.InvestorTradeRankListRes
import com.example.investfeed.kiwoom.sect.dto.rest.res.SectIndexDailyListRes
import com.example.investfeed.kiwoom.sect.dto.rest.res.SectInvestorRes
import com.example.investfeed.kiwoom.sect.dto.rest.res.SectPriceNowRes

data class DashboardRes(
    var kospiPriceRes: SectPriceNowRes? = null,
    var kospiIndexDailyListRes: SectIndexDailyListRes? = null,
    var kospiInvestor: SectInvestorRes? = null,
    var kosdacPriceRes: SectPriceNowRes? = null,
    var kosdacIndexDailyListRes: SectIndexDailyListRes? = null,
    var kosdacInvestor: SectInvestorRes? = null,
    var kospi200PriceRes: SectPriceNowRes? = null,
    var kospi200IndexDailyListRes: SectIndexDailyListRes? = null,
    var investorTradeRankRes : InvestorTradeRankListRes? = null
)