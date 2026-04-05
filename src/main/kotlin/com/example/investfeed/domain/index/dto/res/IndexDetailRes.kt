package com.example.investfeed.domain.index.dto.res

data class IndexDetailRes(
    var indexInfo: IndexInfo? = null,
    var chartList: List<IndexChart>? = null,
    var programChartList: List<ProgramChart>? = null,
    var programList: List<ProgramListItem>? = null,
    var investorDailyList: List<IndexInvestorDailyItem>? = null
)