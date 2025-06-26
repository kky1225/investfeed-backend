package com.example.investfeed.kiwoom.index.dto.res

import com.example.investfeed.kiwoom.sect.dto.rest.res.SectInvestorRes
import com.example.investfeed.kiwoom.sect.dto.rest.res.SectPriceNowRes

data class IndexDetailRes<T>(
    var sectPriceRes: SectPriceNowRes? = null,
    var chartListRes: T? = null,
    var sectInvestor: SectInvestorRes? = null,
)