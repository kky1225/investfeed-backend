package com.example.investfeed.domain.index.dto.res

import com.example.investfeed.kiwoom.sect.dto.res.KiwoomSectInvestorRes
import com.example.investfeed.kiwoom.sect.dto.res.KiwoomSectPriceNowRes

data class IndexDetailRes<T>(
    var sectPriceRes: KiwoomSectPriceNowRes? = null,
    var chartListRes: T? = null,
    var sectInvestor: KiwoomSectInvestorRes? = null,
)