package com.example.investfeed.domain.dashboard.dto.res

data class DashboardIndexListItem(
    var indsCd: String? = null,
    var indsNm: String? = null,
    var curPrc: String? = null,
    var predPreSig: String? = null,
    var fluRt: String? = null,
    var tm: String? = null,
    var ind: String? = null,
    var orgn: String? = null,
    var frgnr: String? = null,
    var chartList: List<ChartDay>? = null,
    var dfrtTrdeNetprps: String? = null,
    var ndiffproTrdeNetprps: String? = null,
    var allNetprps: String? = null,
)