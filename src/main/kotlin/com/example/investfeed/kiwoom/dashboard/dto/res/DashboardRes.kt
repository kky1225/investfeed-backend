package com.example.investfeed.kiwoom.dashboard.dto.res

import com.example.investfeed.kiwoom.investor.dto.res.InvestorTradeDailyRes
import com.example.investfeed.kiwoom.sect.dto.rest.res.SectIndexListRes
import com.example.investfeed.kiwoom.sect.dto.rest.res.SectPriceRes

data class DashboardRes(
    var sectPriceRes: SectPriceRes? = null,
    var investorTradeDailyRes : InvestorTradeDailyRes? = null,
    var sectIndexListRes: SectIndexListRes? = null,
)