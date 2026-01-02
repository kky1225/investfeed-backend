package com.example.investfeed.domain.index.dto.res

data class IndexDetailRes(
    var indexInfo: IndexInfo,
    var chartList: List<IndexChart>,
)