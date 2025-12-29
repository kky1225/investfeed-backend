package com.example.investfeed.domain.index.dto.res

data class IndexListItem(
    var indsCd: String? = null,
    var indsNm: String? = null,
    var curPrc: String? = null,
    var predPreSig: String? = null,
    var fluRt: String? = null,
    var trdeQty: String? = null,
    var trdePrica: String? = null,
    var openPric: String? = null,
    var tmN: String? = null,
    var chartMinuteList: List<ChartMinute>? = null
)