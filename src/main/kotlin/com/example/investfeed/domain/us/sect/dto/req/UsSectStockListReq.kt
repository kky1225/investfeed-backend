package com.example.investfeed.domain.us.sect.dto.req

data class UsSectStockListReq(
    var sortTp: String = "1" // 정렬기준구분 1:등락율상위, 2:등락율하위
)
