package com.example.investfeed.domain.dashboard.dto.res

import com.example.investfeed.kiwoom.investor.dto.res.InvestorTradeRankListRes
import com.example.investfeed.kiwoom.sect.dto.res.KiwoomSectIndexDailyRes
import com.example.investfeed.kiwoom.sect.dto.res.KiwoomSectInvestorRes
import com.example.investfeed.kiwoom.sect.dto.res.KiwoomSectPriceNowRes

data class DashboardRes(
    var kospiPriceRes: KiwoomSectPriceNowRes? = null,
    var kospiIndexDailyListRes: KiwoomSectIndexDailyRes? = null,
    var kospiInvestor: KiwoomSectInvestorRes? = null,
    var kosdacPriceRes: KiwoomSectPriceNowRes? = null,
    var kosdacIndexDailyListRes: KiwoomSectIndexDailyRes? = null,
    var kosdacInvestor: KiwoomSectInvestorRes? = null,
    var kospi200PriceRes: KiwoomSectPriceNowRes? = null,
    var kospi200IndexDailyListRes: KiwoomSectIndexDailyRes? = null,
    var investorTradeRankRes : InvestorTradeRankListRes? = null
)