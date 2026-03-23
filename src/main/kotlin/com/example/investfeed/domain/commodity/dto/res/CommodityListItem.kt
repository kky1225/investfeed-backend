package com.example.investfeed.domain.commodity.dto.res

import com.example.investfeed.domain.index.dto.res.ChartMinute

data class CommodityListItem(
    var stkCd: String? = null,
    var stkNm: String? = null,
    var curPrc: String? = null,
    var predPreSig: String? = null,
    var predPre: String? = null,
    var fluRt: String? = null,
    var trdeQty: String? = null,
    var trdePrica: String? = null,
    var openPric: String? = null,
    var tm: String? = null,
    var chartMinuteList: List<ChartMinute>? = null
)